package io.bartholomews.fsclient.core.oauth.v2

import io.bartholomews.fsclient.core.oauth.{AuthorizationCode, NonRefreshableToken}
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.{Decoder, Error}
import sttp.client.{Identity, RequestT, ResponseAs, ResponseError}
import sttp.model.{QueryParams, Uri}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//  The OAuth 2.0 Authorization Framework
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// https://tools.ietf.org/html/rfc6749
object OAuthV2 {
  type ResponseHandler[T] = ResponseAs[Either[ResponseError[Error], T], Nothing]

  sealed trait SignerType

  case class ClientId(value: String) extends AnyVal
  case class ClientSecret(value: String) extends AnyVal

  case class AccessToken(value: String) extends AnyVal
  object AccessToken { implicit val decoder: Decoder[AccessToken] = deriveUnwrappedDecoder }

  case class RefreshToken(value: String) extends AnyVal
  object RefreshToken { implicit val decoder: Decoder[RefreshToken] = deriveUnwrappedDecoder }

  // https://tools.ietf.org/html/rfc6749#section-3.1.2
  case class RedirectUri(value: Uri)

  // https://tools.ietf.org/html/rfc6749#section-2.3.1
  case class ClientPassword(clientId: ClientId, clientSecret: ClientSecret) {
    def authorizationBasic[U[_], T](request: RequestT[U, T, Nothing]): RequestT[U, T, Nothing] =
      request.auth.basic(clientId.value, clientSecret.value)
    //    lazy val authorizationBasic: Header = FsHeaders.authorizationBasic(s"${clientId.value}:${clientSecret.value}")
  }

  private def authorizationUri(
    responseType: String,
    clientId: ClientId,
    redirectUri: Uri,
    state: Option[String],
    scopes: List[String]
  )(serverUri: Uri): Uri = {
    val requiredQueryParams: List[(String, String)] = List(
      ("client_id", clientId.value),
      ("response_type", responseType),
      ("redirect_uri", redirectUri.toString())
    )

    val optionalQueryParams = List(
      state.map(value => ("state", value)),
      if (scopes.isEmpty) None else Some(scopes.mkString(" ")).map(value => ("scope", value))
    ).flatten

    serverUri
      .params(QueryParams.fromSeq(requiredQueryParams ++ optionalQueryParams))
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //  Authorization Code Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.1
  case object AuthorizationCodeGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.1.1
    def authorizationCodeUri(clientId: ClientId, redirectUri: Uri, state: Option[String], scope: List[String])(
      serverUri: Uri
    ): Uri = authorizationUri(responseType = "code", clientId, redirectUri, state, scope)(serverUri)

    // https://tools.ietf.org/html/rfc6749#section-4.1.3
    def accessTokenRequest(
      uri: Uri,
      code: String,
      maybeRedirectUri: Option[RedirectUri],
      clientPassword: ClientPassword
    )(implicit handleResponse: ResponseHandler[AuthorizationCode]): RequestT[Identity, Either[
      ResponseError[Error],
      AuthorizationCode
    ], Nothing] = { // extends UrlFormRequest.Post[AuthorizationCode] {

      val requestBody: Map[String, String] = Map(
        "grant_type" -> "authorization_code",
        "code" -> code
      ) ++ maybeRedirectUri
        .map(redirectUri => Map("redirect_uri" -> redirectUri.value.toString()))
        .getOrElse(Map.empty)

      val req = sttp.client.emptyRequest
        .post(uri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(requestBody)
        .response(handleResponse)

      println(req.toCurl)
      req
    }

    // https://tools.ietf.org/html/rfc6749#section-6
    def refreshTokenRequest(uri: Uri, refreshToken: RefreshToken, scope: List[String], clientPassword: ClientPassword)(
      implicit handleResponse: ResponseHandler[AuthorizationCode]
    ): RequestT[Identity, Either[ResponseError[Error], AuthorizationCode], Nothing] = {
      // extends UrlFormRequest.Post[AuthorizationCode] {
      //      override val headers: Headers = Headers.of(FsHeaders.contentType(ContentType.APPLICATION_FORM_URLENCODED))
      //      println(refreshToken)
      //      println(scope)

      val requestBody: Map[String, String] = Map(
        "grant_type" -> "refresh_token",
        "refresh_token" -> refreshToken.value
      ) ++ (if (scope.isEmpty) Map.empty else Map("scope" -> scope.mkString(",")))

      println(uri)

      val req = sttp.client.emptyRequest
        .post(uri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(requestBody)
        .response(handleResponse)

      println(req.toCurl)
      req
    }
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Implicit Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.2
  case object ImplicitGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.2.1
    def authorizationTokenUri(clientId: ClientId, redirectUri: Uri, state: Option[String], scopes: List[String])(
      serverUri: Uri
    ): Uri = authorizationUri(responseType = "token", clientId, redirectUri, state, scopes)(serverUri)
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Client Credentials Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.4
  case object ClientCredentialsGrant extends SignerType {
    // https://tools.ietf.org/html/rfc6749#section-4.4.2
    def accessTokenRequest(uri: Uri, clientPassword: ClientPassword)(implicit
      handleResponse: ResponseHandler[NonRefreshableToken]
    ): RequestT[Identity, Either[ResponseError[Error], NonRefreshableToken], Nothing] = { // extends UrlFormRequest.Post[NonRefreshableToken] {
      val requestBody = Map("grant_type" -> "client_credentials")
      sttp.client.emptyRequest
        .post(uri)
        .auth
        .basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
        .body(requestBody)
        .response(handleResponse)
//      override val headers: Headers = Headers.of(FsHeaders.contentType(ContentType.APPLICATION_FORM_URLENCODED))
    }
  }
}
