package io.bartholomews.fsclient.core.oauth.v2

import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner, Scope}
import sttp.client3.{emptyRequest, Identity, RequestT, ResponseAs, ResponseException}
import sttp.model.Uri

import scala.util.Try

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//  The OAuth 2.0 Authorization Framework
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// https://tools.ietf.org/html/rfc6749
object OAuthV2 {
  type ResponseHandler[+E, T] = ResponseAs[Either[ResponseException[String, E], T], Any]

  sealed trait SignerType

  case class AccessToken(value: String) extends AnyVal

  case class RefreshToken(value: String) extends AnyVal

  // https://tools.ietf.org/html/rfc6749#section-3.1.2
  case class RedirectUri(value: Uri)

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //  Authorization Code Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.1
  case object AuthorizationCodeGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.1.1
    def authorizationRequestUri(request: AuthorizationCodeRequest, serverUri: Uri): Uri =
      request.uri(serverUri)

    // https://tools.ietf.org/html/rfc6749#section-4.1.2
    // TODO: `AuthorizationError` type
    def authorizationResponse(
      request: AuthorizationCodeRequest,
      redirectionUriResponse: Uri
    ): Either[String, AuthorizationCode] = {
      val params = redirectionUriResponse.paramsMap

      def maybeCode: Either[String, AuthorizationCode] = params
        .collectFirst({
          case ("code", code)   => Right(AuthorizationCode(code))
          case ("error", error) => Left(error)
        })
        .toRight("missing_required_query_parameters")
        .joinRight

      request.state.fold(maybeCode) { stateRequest =>
        params
          .collectFirst({ case ("state", stateResponse) =>
            if (stateRequest == stateResponse) maybeCode
            else Left("state_parameter_mismatch")
          })
          .getOrElse(Left("missing_required_state_parameter"))
      }
    }

    // https://tools.ietf.org/html/rfc6749#section-4.1.3
    def accessTokenRequest[DE](
      serverUri: Uri,
      code: AuthorizationCode,
      maybeRedirectUri: Option[RedirectUri],
      clientPassword: ClientPassword
    )(implicit handleResponse: ResponseHandler[DE, AccessTokenSigner]): RequestT[Identity, Either[
      ResponseException[String, DE],
      AccessTokenSigner
    ], Any] = {

      val requestBody: Map[String, String] = Map(
        "grant_type" -> "authorization_code",
        "code" -> code.value
      ) ++ maybeRedirectUri
        .map(redirectUri => Map("redirect_uri" -> redirectUri.value.toString))
        .getOrElse(Map.empty)

      emptyRequest
        .post(serverUri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(requestBody)
        .response(handleResponse)
    }

    // https://tools.ietf.org/html/rfc6749#section-6
    def refreshTokenRequest[DE](
      serverUri: Uri,
      refreshToken: RefreshToken,
      scopes: List[String],
      clientPassword: ClientPassword
    )(implicit
      handleResponse: ResponseHandler[DE, AccessTokenSigner]
    ): RequestT[Identity, Either[ResponseException[String, DE], AccessTokenSigner], Any] = {
      val requestBody: Map[String, String] = Map(
        "grant_type" -> "refresh_token",
        "refresh_token" -> refreshToken.value
      ) ++ (if (scopes.isEmpty) Map.empty else Map("scope" -> scopes.mkString(",")))

      emptyRequest
        .post(serverUri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(requestBody)
        .response(handleResponse)
    }
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Implicit Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.2
  case object ImplicitGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.2.1
    def authorizationRequestUri(request: AuthorizationTokenRequest, serverUri: Uri): Uri =
      request.uri(serverUri)

    // https://tools.ietf.org/html/rfc6749#section-4.2.2
    def accessTokenResponse(
      request: AuthorizationTokenRequest,
      redirectionUriResponse: Uri
    ): Either[String, NonRefreshableTokenSigner] = {
      val params = redirectionUriResponse.paramsMap

      def extractToken: Either[String, NonRefreshableTokenSigner] =
        params
          .collectFirst({ case ("error", error) => Left(error) })
          .getOrElse(
            for {
              accessToken <- params.get("access_token").toRight("missing_access_token")
              tokenType <- params.get("token_type").toRight("missing_token_type")
              expiresIn <- params.get("expires_in").toRight("missing_expires_in").flatMap { str =>
                Try(str.toLong).toEither.left.map(_ => "invalid_expires_in")
              }
              scopes <- Right(params.get("scope").toList.flatMap(_.split(" ")))
            } yield NonRefreshableTokenSigner.apply(
              generatedAt = System.nanoTime(),
              accessToken = AccessToken(accessToken),
              tokenType = tokenType,
              expiresIn = expiresIn,
              scope = Scope.apply(scopes)
            )
          )

      request.state.fold(extractToken) { stateRequest =>
        params
          .collectFirst({ case ("state", stateResponse) =>
            if (stateRequest == stateResponse) extractToken
            else Left("state_parameter_mismatch")
          })
          .getOrElse(Left("missing_required_state_parameter"))
      }
    }
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Client Credentials Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.4
  case object ClientCredentialsGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.4.2
    def accessTokenRequest[DE](serverUri: Uri, clientPassword: ClientPassword)(implicit
      handleResponse: ResponseHandler[DE, NonRefreshableTokenSigner]
    ): RequestT[Identity, Either[
      ResponseException[String, DE],
      NonRefreshableTokenSigner
    ], Any] = // extends UrlFormRequest.Post[NonRefreshableToken] {
      emptyRequest
        .post(serverUri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(Map("grant_type" -> "client_credentials"))
        .response(handleResponse)
  }
}
