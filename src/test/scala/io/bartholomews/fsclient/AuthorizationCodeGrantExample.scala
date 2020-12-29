package io.bartholomews.fsclient

object AuthorizationCodeGrantExample extends App {

  import io.bartholomews.fsclient.core.FsClient
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AuthorizationCodeGrant, RedirectUri}
  import io.bartholomews.fsclient.core.oauth.v2.{
    AuthorizationCode,
    AuthorizationCodeRequest,
    ClientId,
    ClientPassword,
    ClientSecret,
    OAuthV2
  }
  import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, ClientPasswordAuthentication}
  import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, ResponseError, SttpBackend, UriContext}
  import sttp.model.Uri

  def dealWithIt = throw new Exception("¯x--(ツ)--x")

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

  val userAgent = UserAgent(
    appName = "SAMPLE_APP_NAME",
    appVersion = Some("SAMPLE_APP_VERSION"),
    appUrl = Some("https://bartholomews.io/sample-app-url")
  )

  // you probably want to load this from config
  val myClientPassword = ClientPassword(
    clientId = ClientId("APP_CLIENT_ID"),
    clientSecret = ClientSecret("APP_CLIENT_SECRET")
  )

  val myRedirectUri = RedirectUri(uri"https://my-app/callback")

  val client = FsClient.v2.clientPassword(userAgent, ClientPasswordAuthentication(myClientPassword))

  // 1. Prepare an authorization code request
  val authorizationCodeRequest = AuthorizationCodeRequest(
    clientId = client.signer.clientPassword.clientId,
    redirectUri = myRedirectUri,
    state = Some("some-state"), // see https://tools.ietf.org/html/rfc6749#section-10.12
    scopes = List.empty // see https://tools.ietf.org/html/rfc6749#section-3.3
  )

  /*
     2. Send the user to `authorizationRequestUri`,
     where they will accept/deny permissions for our client app to access their data;
     they will be then redirected to `authorizationCodeRequest.redirectUri`
   */
  val authorizationRequestUri: Uri = AuthorizationCodeGrant.authorizationRequestUri(
    request = authorizationCodeRequest,
    serverUri = uri"https://some-authorization-server/authorize"
  )

  // a successful `redirectionUriResponse` will look like this:
  val redirectionUriResponseApproved = uri"https://my-app/callback?code=some-auth-code-verifier&state=some-state"

  // 3. Validate `redirectionUriResponse`
  val maybeAuthorizationCode: Either[String, AuthorizationCode] = AuthorizationCodeGrant.authorizationResponse(
    request = authorizationCodeRequest,
    redirectionUriResponse = redirectionUriResponseApproved
  )

  /*
    Trying to move out from circe as default and have multi-modules json libs instead,
    which would provide these implicits out of the box for their own codecs,
    so for now you need to explicitly provide one
   */
  implicit val handleResponse: OAuthV2.ResponseHandler[AccessTokenSigner] =
    sttp.client.circe.asJson[AccessTokenSigner]

  // 4. Get an access token
  val maybeToken: Either[ResponseError[io.circe.Error], AccessTokenSigner] = AuthorizationCodeGrant
    .accessTokenRequest(
      uri = uri"https://some-authorization-server/token",
      code = maybeAuthorizationCode.getOrElse(dealWithIt),
      maybeRedirectUri = Some(myRedirectUri),
      clientPassword = myClientPassword
    )
    .send()
    .body

  implicit val accessToken: AccessTokenSigner = maybeToken.getOrElse(dealWithIt)

  // 5. Use the access token
  import io.bartholomews.fsclient.core.http.FsClientSttpExtensions._
  // an empty request with client `User-Agent` header
  baseRequest(client)
    .get(uri"https://some-server/authenticated-endpoint")
    .sign // sign with the implicit token provided

  // 6. Get a refresh token
  if (accessToken.isExpired() && accessToken.refreshToken.isDefined) {
    AuthorizationCodeGrant
      .refreshTokenRequest(
        uri = uri"https://some-authorization-server/refresh",
        accessToken.refreshToken.getOrElse(dealWithIt),
        scopes = accessToken.scope.values,
        clientPassword = myClientPassword
      )
      .send()
  }
}
