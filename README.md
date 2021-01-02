[![CircleCI](https://circleci.com/gh/bartholomews/fsclient/tree/master.svg?style=svg)](https://circleci.com/gh/bartholomews/fsclient/tree/master)
[![codecov](https://codecov.io/gh/bartholomews/fsclient/branch/master/graph/badge.svg)](https://codecov.io/gh/bartholomews/fsclient)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-black.svg)](http://unlicense.org/)

# fsclient

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.bartholomews/fsclient_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.bartholomews/fsclient_2.13)

```
libraryDependencies += "io.bartholomews" %% "fsclient" % "0.1.0"
```

http client wrapping [sttp](https://sttp.softwaremill.com/en/stable) 
and providing OAuth signatures and other utils

```scala
  import io.bartholomews.fsclient.core._
  import io.bartholomews.fsclient.core.oauth.Signer
  import sttp.client._

  implicit val signer: Signer = ???

  /*
    Sign the sttp request with `Signer`, which might be
    an OAuth v1 signature, or OAuth v2 basic / bearer, or a custom `Authorization` header.
   */
  emptyRequest
    .get(uri"https://some-server/authenticated-endpoint")
    .sign
```

### [OAuth 1.0](https://tools.ietf.org/html/rfc5849)

#### Token Credentials

```scala
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v1.OAuthV1.{Consumer, SignatureMethod}
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v1.TemporaryCredentials
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.OAuthV2.RedirectUri
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.{
    RequestTokenCredentials,
    ResourceOwnerAuthorizationUri,
    TemporaryCredentialsRequest
  }
  import sttp.client.{
    emptyRequest,
    DeserializationError,
    HttpURLConnectionBackend,
    Identity,
    NothingT,
    Response,
    ResponseError,
    SttpBackend,
    UriContext
  }
  import sttp.model.Method

  type F[X] = Identity[X]

  def dealWithIt = throw new Exception("¯x--(ツ)--x")

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()

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
  val maybeTemporaryCredentials: F[Response[Either[ResponseError[Exception], TemporaryCredentials]]] =
    temporaryCredentialsRequest.send(
      Method.POST,
      // https://tools.ietf.org/html/rfc5849#section-2.1
      serverUri = uri"https://some-authorization-server/io.bartholomews.fsclient.circe.oauth/request-token",
      userAgent,
      // https://tools.ietf.org/html/rfc5849#section-2.2
      ResourceOwnerAuthorizationUri(uri"https://some-server/io.bartholomews.fsclient.circe.oauth/authorize")
    )

  // a successful `redirectionUriResponse` will have the token in the query parameters:
  val redirectionUriResponseApproved =
    RedirectUri(myRedirectUri.value.params(("oauth_token", "AAA"), ("oauth_verifier", "ZZZ")))

  // 3. Get the Token Credentials
  val maybeRequestTokenCredentials: Either[DeserializationError[Exception], RequestTokenCredentials] =
    RequestTokenCredentials.validate(
      redirectionUriResponseApproved,
      maybeTemporaryCredentials.body.getOrElse(dealWithIt),
      SignatureMethod.PLAINTEXT
    )

  implicit val requestToken: RequestTokenCredentials = maybeRequestTokenCredentials.getOrElse(dealWithIt)

  // 4. Use the Token Credentials
  import io.bartholomews.fsclient.core._
  emptyRequest
    .get(uri"https://some-server/authenticated-endpoint")
    .sign // sign with the implicit token provided
```

### [OAuth 2.0](https://tools.ietf.org/html/rfc6749)

#### Client credentials

```scala
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.NonRefreshableTokenSigner
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.OAuthV2.ClientCredentialsGrant
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.{ClientId, ClientPassword, ClientSecret, OAuthV2}
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
```

#### Implicit grant

```scala
  import io.bartholomews.fsclient.core.FsClient
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.{ClientPasswordAuthentication, NonRefreshableTokenSigner}
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.OAuthV2.{ImplicitGrant, RedirectUri}
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.{AuthorizationTokenRequest, ClientId, ClientPassword, ClientSecret}
  import sttp.client.{emptyRequest, HttpURLConnectionBackend, Identity, NothingT, SttpBackend, UriContext}
  import sttp.model.Uri

  def dealWithIt = throw new Exception("¯x--(ツ)--x")

  implicit val backend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  
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

  val client = FsClient.v2.clientPassword(userAgent, ClientPasswordAuthentication(myClientPassword))

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
     they will be then redirected to `authorizationCodeRequest.redirectUri`
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

  // 5. Use the access token
  import io.bartholomews.fsclient.core._
  emptyRequest
    .get(uri"https://some-server/authenticated-endpoint")
    .sign(maybeToken.getOrElse(dealWithIt)) // sign with the implicit token provided
```

#### Authorization code grant

```scala
  import io.bartholomews.fsclient.core.FsClient
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.OAuthV2.{AuthorizationCodeGrant, RedirectUri}
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.v2.{
    AuthorizationCode,
    AuthorizationCodeRequest,
    ClientId,
    ClientPassword,
    ClientSecret,
    OAuthV2
  }
  import io.bartholomews.fsclient.core.io.bartholomews.fsclient.circe.oauth.{AccessTokenSigner, ClientPasswordAuthentication}
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
      serverUri = uri"https://some-authorization-server/token",
      code = maybeAuthorizationCode.getOrElse(dealWithIt),
      maybeRedirectUri = Some(myRedirectUri),
      clientPassword = myClientPassword
    )
    .send()
    .body

  implicit val accessToken: AccessTokenSigner = maybeToken.getOrElse(dealWithIt)

  // 5. Use the access token
  import io.bartholomews.fsclient.core._
  // an empty request with client `User-Agent` header
  baseRequest(client)
    .get(uri"https://some-server/authenticated-endpoint")
    .sign // sign with the implicit token provided

  // 6. Get a refresh token
  if (accessToken.isExpired() && accessToken.refreshToken.isDefined) {
    AuthorizationCodeGrant
      .refreshTokenRequest(
        serverUri = uri"https://some-authorization-server/refresh",
        accessToken.refreshToken.getOrElse(dealWithIt),
        scopes = accessToken.scope.values,
        clientPassword = myClientPassword
      )
      .send()
  }
```

## CircleCI deployment

### Verify local configuration
https://circleci.com/docs/2.0/local-cli/
```bash
circleci config validate
```

### CI/CD Pipeline

This project is using [sbt-ci-release](https://github.com/olafurpg/sbt-ci-release) plugin:
 - Every push to master will trigger a snapshot release.  
 - In order to trigger a regular release you need to push a tag:
 
    ```bash
    ./scripts/release.sh v1.0.0
    ```
 
 - If for some reason you need to replace an older version (e.g. the release stage failed):
 
    ```bash
    TAG=v1.0.0
    git push --delete origin ${TAG} && git tag --delete ${TAG} \
    && ./scripts/release.sh ${TAG}
    ```