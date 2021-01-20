package io.bartholomews.fsclient.samples.v1

object TokenSignatureExample extends App {
  import io.bartholomews.fsclient.core.oauth.{
    RequestTokenCredentials,
    ResourceOwnerAuthorizationUri,
    TemporaryCredentialsRequest
  }
  import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, SignatureMethod}
  import io.bartholomews.fsclient.core.oauth.v1.TemporaryCredentials
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.RedirectUri
  import io.bartholomews.fsclient.client.ClientData.sampleRedirectUri
  import sttp.client3.{
    emptyRequest,
    DeserializationException,
    HttpURLConnectionBackend,
    Identity,
    Response,
    ResponseException,
    SttpBackend,
    UriContext
  }
  import io.bartholomews.fsclient.core.config.UserAgent
  import sttp.model.Method

  type F[X] = Identity[X]

  def dealWithIt = throw new Exception("¯x--(ツ)--x")

  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  val userAgent = UserAgent(
    appName = "SAMPLE_APP_NAME",
    appVersion = Some("SAMPLE_APP_VERSION"),
    appUrl = Some("https://bartholomews.io/sample-app-url")
  )

  // you probably want to load this from config
  val myConsumer: Consumer = Consumer(
    key = "CONSUMER_KEY",
    secret = "CONSUMER_SECRET"
  )

  val myRedirectUri = RedirectUri(uri"https://my-app/callback")

  // 1. Prepare a temporary credentials request
  val temporaryCredentialsRequest = TemporaryCredentialsRequest(
    myConsumer,
    myRedirectUri,
    SignatureMethod.SHA1
  )

  // 2. Retrieve temporary credentials
  val maybeTemporaryCredentials: F[Response[Either[ResponseException[String, Exception], TemporaryCredentials]]] =
    temporaryCredentialsRequest.send(
      Method.POST,
      // https://tools.ietf.org/html/rfc5849#section-2.1
      serverUri = uri"https://some-authorization-server/oauth/request-token",
      userAgent,
      // https://tools.ietf.org/html/rfc5849#section-2.2
      ResourceOwnerAuthorizationUri(uri"https://some-server/oauth/authorize")
    )

  // a successful `resourceOwnerAuthorizationUriResponse` will have the token in the query parameters:
  val resourceOwnerAuthorizationUriResponse =
    sampleRedirectUri.value.withParams(("oauth_token", "AAA"), ("oauth_verifier", "ZZZ"))

  // 3. Get the Token Credentials
  val maybeRequestTokenCredentials: Either[DeserializationException[Exception], RequestTokenCredentials] =
    RequestTokenCredentials.fetchRequestTokenCredentials(
      resourceOwnerAuthorizationUriResponse,
      maybeTemporaryCredentials.body.getOrElse(dealWithIt),
      SignatureMethod.PLAINTEXT
    )

  implicit val requestToken: RequestTokenCredentials = maybeRequestTokenCredentials.getOrElse(dealWithIt)

  // import `FsClientSttpExtensions` in http package to use `sign`
  import io.bartholomews.fsclient.core._

  // 4. Use the Token Credentials
  emptyRequest
    .get(uri"https://some-server/authenticated-endpoint")
    .sign // sign with the implicit token provided
}
