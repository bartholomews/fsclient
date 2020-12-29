package io.bartholomews.fsclient.core.oauth.v2

import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner}
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.{Decoder, Error}
import sttp.client.{Identity, RequestT, ResponseAs, ResponseError}
import sttp.model.Uri

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//  The OAuth 2.0 Authorization Framework
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// https://tools.ietf.org/html/rfc6749
object OAuthV2 {
  type ResponseHandler[T] = ResponseAs[Either[ResponseError[Error], T], Nothing]

  sealed trait SignerType

  case class AccessToken(value: String) extends AnyVal
  object AccessToken { implicit val decoder: Decoder[AccessToken] = deriveUnwrappedDecoder }

  case class RefreshToken(value: String) extends AnyVal
  object RefreshToken { implicit val decoder: Decoder[RefreshToken] = deriveUnwrappedDecoder }

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
      val responseParams = redirectionUriResponse.paramsSeq

      val maybeCode: Either[String, AuthorizationCode] = responseParams
        .collectFirst({
          case ("code", code)   => Right(AuthorizationCode(code))
          case ("error", error) => Left(error)
        })
        .toRight("missing_required_query_parameters")
        .joinRight

      request.state.fold(maybeCode) { stateRequest =>
        responseParams
          .collectFirst({ case ("state", stateResponse) =>
            if (stateRequest == stateResponse) maybeCode
            else Left("state_parameter_mismatch")
          })
          .getOrElse(Left("missing_required_state_parameter"))
      }
    }

    // https://tools.ietf.org/html/rfc6749#section-4.1.3
    def accessTokenRequest(
      uri: Uri,
      code: AuthorizationCode,
      maybeRedirectUri: Option[RedirectUri],
      clientPassword: ClientPassword
    )(implicit handleResponse: ResponseHandler[AccessTokenSigner]): RequestT[Identity, Either[
      ResponseError[Error],
      AccessTokenSigner
    ], Nothing] = {

      val requestBody: Map[String, String] = Map(
        "grant_type" -> "authorization_code",
        "code" -> code.value
      ) ++ maybeRedirectUri
        .map(redirectUri => Map("redirect_uri" -> redirectUri.value.toString))
        .getOrElse(Map.empty)

      sttp.client.emptyRequest
        .post(uri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(requestBody)
        .response(handleResponse)
    }

    // https://tools.ietf.org/html/rfc6749#section-6
    def refreshTokenRequest(uri: Uri, refreshToken: RefreshToken, scopes: List[String], clientPassword: ClientPassword)(
      implicit handleResponse: ResponseHandler[AccessTokenSigner]
    ): RequestT[Identity, Either[ResponseError[Error], AccessTokenSigner], Nothing] = {
      val requestBody: Map[String, String] = Map(
        "grant_type" -> "refresh_token",
        "refresh_token" -> refreshToken.value
      ) ++ (if (scopes.isEmpty) Map.empty else Map("scope" -> scopes.mkString(",")))

      sttp.client.emptyRequest
        .post(uri)
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
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Client Credentials Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.4
  case object ClientCredentialsGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.4.2
    def accessTokenRequest(uri: Uri, clientPassword: ClientPassword)(implicit
      handleResponse: ResponseHandler[NonRefreshableTokenSigner]
    ): RequestT[Identity, Either[
      ResponseError[Error],
      NonRefreshableTokenSigner
    ], Nothing] = // extends UrlFormRequest.Post[NonRefreshableToken] {
      sttp.client.emptyRequest
        .post(uri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(Map("grant_type" -> "client_credentials"))
        .response(handleResponse)
  }
}
