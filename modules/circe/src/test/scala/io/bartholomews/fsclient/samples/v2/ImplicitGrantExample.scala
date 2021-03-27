package io.bartholomews.fsclient.samples.v2

object ImplicitGrantExample extends App {

  import io.bartholomews.fsclient.core.FsClient
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{ImplicitGrant, RedirectUri}
  import io.bartholomews.fsclient.core.oauth.v2.{AuthorizationTokenRequest, ClientId, ClientPassword, ClientSecret}
  import io.bartholomews.fsclient.core.oauth.{ClientPasswordAuthentication, NonRefreshableTokenSigner}
  import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend, UriContext, emptyRequest}
  import sttp.model.Uri

  val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  val userAgent: UserAgent = UserAgent(
    appName = "SAMPLE_APP_NAME",
    appVersion = Some("SAMPLE_APP_VERSION"),
    appUrl = Some("https://bartholomews.io/sample-app-url")
  )

  // you probably want to load this from config
  val myClientPassword: ClientPassword = ClientPassword(
    clientId = ClientId("APP_CLIENT_ID"),
    clientSecret = ClientSecret("APP_CLIENT_SECRET")
  )

  val myRedirectUri = RedirectUri(uri"https://my-app/callback")

  val client = FsClient.v2.clientPassword(userAgent, ClientPasswordAuthentication(myClientPassword))(backend)

  // 1. Prepare an authorization token request
  val authorizationTokenRequest = AuthorizationTokenRequest(
    clientId = client.signer.clientPassword.clientId,
    redirectUri = myRedirectUri,
    state = Some("some-state"), // see https://tools.ietf.org/html/rfc6749#section-10.12
    scopes = List.empty // see https://tools.ietf.org/html/rfc6749#section-3.3
  )

  /*
     2. Send the user to `authorizationRequestUri`,
     where they will accept/deny permissions for our client app to access their data;
     they will be then redirected to `AuthorizationTokenRequest.redirectUri`
   */
  val authorizationRequestUri: Uri = ImplicitGrant.authorizationRequestUri(
    request = authorizationTokenRequest,
    serverUri = uri"https://some-authorization-server/authorize"
  )

  // a successful `redirectionUriResponse` will have the token in the query parameters:
  val redirectionUriResponseApproved =
    uri"https://my-app/callback?access_token=some-token-verifier&token_type=bearer&state=some-state"

  // 4. Get an access token
  val maybeToken: Either[String, NonRefreshableTokenSigner] = ImplicitGrant
    .accessTokenResponse(
      request = authorizationTokenRequest,
      redirectionUriResponse = redirectionUriResponseApproved
    )

  maybeToken.map(token => {
    // import `FsClientSttpExtensions` in http package to use `sign`
    import io.bartholomews.fsclient.core._

    // 5. Use the access token
    emptyRequest
      .get(uri"https://some-server/authenticated-endpoint")
      .sign(token) // sign with the token provided
  })
}
