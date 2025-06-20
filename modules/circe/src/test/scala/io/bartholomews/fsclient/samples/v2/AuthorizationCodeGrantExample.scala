//package io.bartholomews.fsclient.samples.v2
//
//import io.bartholomews.fsclient.core.http.FsClientSttpExtensions.baseRequest
//
//object AuthorizationCodeGrantExample extends App {
//  import io.bartholomews.fsclient.core._
//  import io.bartholomews.fsclient.core.config.UserAgent
//  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AuthorizationCodeGrant, RefreshToken}
//  import io.bartholomews.fsclient.core.oauth.v2._
//  import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, ClientPasswordAuthentication, RedirectUri}
//  import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend, UriContext}
//  import sttp.model.Uri
//
//  val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
//
//  val userAgent = UserAgent(
//    appName = "SAMPLE_APP_NAME",
//    appVersion = Some("SAMPLE_APP_VERSION"),
//    appUrl = Some("https://bartholomews.io/sample-app-url")
//  )
//
//  // you probably want to load this from config
//  val myClientPassword = ClientPassword(
//    clientId = ClientId("APP_CLIENT_ID"),
//    clientSecret = ClientSecret("APP_CLIENT_SECRET")
//  )
//
//  val myRedirectUri = RedirectUri(uri"https://my-app/callback")
//
//  val client = FsClient.v2.clientPassword(userAgent, ClientPasswordAuthentication(myClientPassword))(backend)
//
//  // 1. Prepare an authorization code request
//  val authorizationCodeRequest = AuthorizationCodeRequest(
//    clientId = client.signer.clientPassword.clientId,
//    redirectUri = myRedirectUri,
//    state = Some("some-state"), // see https://tools.ietf.org/html/rfc6749#section-10.12
//    scopes = List.empty // see https://tools.ietf.org/html/rfc6749#section-3.3
//  )
//
//  /*
//     2. Send the user to `authorizationRequestUri`,
//     where they will accept/deny permissions for our client app to access their data;
//     they will be then redirected to `authorizationCodeRequest.redirectUri`
//   */
//  val authorizationRequestUri: Uri = AuthorizationCodeGrant.authorizationRequestUri(
//    request = authorizationCodeRequest,
//    serverUri = uri"https://some-authorization-server/authorize"
//  )
//
//  // a successful `redirectionUriResponse` will look like this:
//  val redirectionUriResponseApproved = uri"https://my-app/callback?code=some-auth-code-verifier&state=some-state"
//
//  // 3. Validate `redirectionUriResponse`
//  val maybeAuthorizationCode: Either[String, AuthorizationCode] = AuthorizationCodeGrant.authorizationResponse(
//    request = authorizationCodeRequest,
//    redirectionUriResponse = redirectionUriResponseApproved
//  )
//
//  // using fsclient-circe codecs
//  import io.bartholomews.fsclient.circe.codecs._
//
//  // 4. Get an access token
//  val maybeToken: Either[String, AccessTokenSigner] =
//    maybeAuthorizationCode.flatMap { authorizationCode =>
//      backend
//        .send(
//          AuthorizationCodeGrant
//            .accessTokenRequest(
//              serverUri = uri"https://some-authorization-server/token",
//              code = authorizationCode,
//              maybeRedirectUri = Some(myRedirectUri),
//              clientPassword = myClientPassword
//            )
//        )
//        .body
//        .left
//        .map(_.getMessage)
//    }
//
//  maybeToken.map { accessTokenSigner =>
//    // 5. Use the access token
//    baseRequest(userAgent)
//      .get(uri"https://some-server/authenticated-endpoint")
//      .sign(accessTokenSigner) // sign with the token signer
//
//    // 6. Get a refresh token
//    if (accessTokenSigner.isExpired()) {
//      backend.send(
//        AuthorizationCodeGrant
//          .refreshTokenRequest(
//            serverUri = uri"https://some-authorization-server/refresh",
//            accessTokenSigner.refreshToken.getOrElse(
//              RefreshToken(
//                "Refresh token is optional: some implementations (e.g. Spotify) only give you a refresh token " +
//                  "with the first `accessTokenSigner` authorization response, so you might need to store and use that."
//              )
//            ),
//            scopes = accessTokenSigner.scope.values,
//            clientPassword = myClientPassword
//          )
//      )
//    }
//  }
//}
