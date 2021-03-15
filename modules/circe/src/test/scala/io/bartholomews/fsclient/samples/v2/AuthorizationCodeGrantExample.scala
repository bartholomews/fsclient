package io.bartholomews.fsclient.samples.v2

import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.RefreshToken

object AuthorizationCodeGrantExample extends App {
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, ClientPasswordAuthentication}
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AuthorizationCodeGrant, RedirectUri}
  import io.bartholomews.fsclient.core.oauth.v2.{
    AuthorizationCode,
    AuthorizationCodeRequest,
    ClientId,
    ClientPassword,
    ClientSecret
  }
  import sttp.client3.{HttpURLConnectionBackend, Identity, ResponseException, SttpBackend, UriContext}
  import sttp.model.Uri
  import io.bartholomews.fsclient.core._

  def dealWithIt = throw new Exception("¯x--(ツ)--x")

  val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

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

  val client = FsClient.v2.clientPassword(userAgent, ClientPasswordAuthentication(myClientPassword))(backend)

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

  // using fsclient-circe codecs
  import io.bartholomews.fsclient.circe.codecs._

  // 4. Get an access token
  val maybeToken: Either[ResponseException[String, io.circe.Error], AccessTokenSigner] =
    backend
      .send(
        AuthorizationCodeGrant
          .accessTokenRequest[io.circe.Error](
            serverUri = uri"https://some-authorization-server/token",
            code = maybeAuthorizationCode.getOrElse(dealWithIt),
            maybeRedirectUri = Some(myRedirectUri),
            clientPassword = myClientPassword
          )
      )
      .body

  implicit val accessTokenSigner: AccessTokenSigner = maybeToken.getOrElse(dealWithIt)

  // 5. Use the access token
  // an empty request with client `User-Agent` header
  baseRequest(userAgent)
    .get(uri"https://some-server/authenticated-endpoint")
    .sign // sign with the implicit token provided

  // 6. Get a refresh token
  if (accessTokenSigner.isExpired()) {
    backend.send(
      AuthorizationCodeGrant
        .refreshTokenRequest(
          serverUri = uri"https://some-authorization-server/refresh",
          accessTokenSigner.refreshToken.getOrElse(
            RefreshToken(
              "Refresh token is optional: some implementations (e.g. Spotify) only give you a refresh token " +
                "with the first `accessTokenSigner` authorization response, so you might need to store and use that."
            )
          ),
          scopes = accessTokenSigner.scope.values,
          clientPassword = myClientPassword
        )
    )
  }
}
