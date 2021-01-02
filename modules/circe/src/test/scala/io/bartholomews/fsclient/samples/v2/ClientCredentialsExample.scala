package io.bartholomews.fsclient.samples.v2

object ClientCredentialsExample extends App {
  import io.bartholomews.fsclient.core.oauth.NonRefreshableTokenSigner
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ClientCredentialsGrant
  import io.bartholomews.fsclient.core.oauth.v2.{ClientId, ClientPassword, ClientSecret}
  import io.circe
  import sttp.client3.{HttpURLConnectionBackend, Identity, Response, ResponseException, SttpBackend, UriContext}

  type F[X] = Identity[X]

  implicit val backend: SttpBackend[F, Any] = HttpURLConnectionBackend()

  // using fsclient-circe codecs
  import io.bartholomews.fsclient.circe._

  // you probably want to load this from config
  val myClientPassword = ClientPassword(
    clientId = ClientId("APP_CLIENT_ID"),
    clientSecret = ClientSecret("APP_CLIENT_SECRET")
  )

  val accessTokenRequest: F[Response[Either[ResponseException[String, circe.Error], NonRefreshableTokenSigner]]] =
    backend.send(
      ClientCredentialsGrant
        .accessTokenRequest(
          serverUri = uri"https://some-authorization-server/token",
          myClientPassword
        )
    )
}
