package io.bartholomews.fsclient.samples.v2

object ClientCredentialsExample extends App {
  import io.bartholomews.fsclient.core.oauth.NonRefreshableTokenSigner
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ClientCredentialsGrant
  import io.bartholomews.fsclient.core.oauth.v2.{ClientId, ClientPassword, ClientSecret, OAuthV2}
  import io.circe
  import sttp.client.{HttpURLConnectionBackend, Identity, NothingT, Response, ResponseError, SttpBackend, UriContext}

  type F[X] = Identity[X]

  implicit val backend: SttpBackend[F, Nothing, NothingT] = HttpURLConnectionBackend()

  /*
  Trying to move out from circe as default and have multi-modules json libs instead,
  which would provide these implicits out of the box for their own codecs,
  so for now you need to explicitly provide one
   */
  implicit val handleResponse: OAuthV2.ResponseHandler[NonRefreshableTokenSigner] =
    sttp.client.circe.asJson[NonRefreshableTokenSigner]

  // you probably want to load this from config
  val myClientPassword = ClientPassword(
    clientId = ClientId("APP_CLIENT_ID"),
    clientSecret = ClientSecret("APP_CLIENT_SECRET")
  )

  val accessTokenRequest: F[Response[Either[ResponseError[circe.Error], NonRefreshableTokenSigner]]] =
    ClientCredentialsGrant
      .accessTokenRequest(
        serverUri = uri"https://some-authorization-server/token",
        myClientPassword
      )
      .send()
}
