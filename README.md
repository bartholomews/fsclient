[![CircleCI](https://circleci.com/gh/bartholomews/fsclient/tree/master.svg?style=svg)](https://circleci.com/gh/bartholomews/fsclient/tree/master)
[![codecov](https://codecov.io/gh/bartholomews/fsclient/branch/master/graph/badge.svg)](https://codecov.io/gh/bartholomews/fsclient)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![License: Unlicense](https://img.shields.io/badge/license-Unlicense-black.svg)](http://unlicense.org/)

# fsclient

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.bartholomews/fsclient-core_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.bartholomews/fsclient-core_2.13)

```
// circe codecs
libraryDependencies += "io.bartholomews" %% "fsclient-circe" % "0.1.1"
// play-json codecs
libraryDependencies += "io.bartholomews" %% "fsclient-play" % "0.1.1"
// no codecs
libraryDependencies += "io.bartholomews" %% "fsclient-core" % "0.1.1"
```

http client wrapping [sttp](https://sttp.softwaremill.com/en/latest) 
and providing OAuth signatures and other utils

```scala
  import io.bartholomews.fsclient.core._
  import io.bartholomews.fsclient.core.oauth.Signer
  import sttp.client3._

  implicit val signer: Signer = ???

  /*
    Sign the sttp request with `Signer`, which might be one of:
    - an OAuth v1 signature
    - an OAuth v2 basic / bearer
    - a custom `Authorization` header
   */
  emptyRequest
    .get(uri"https://some-server/authenticated-endpoint")
    .sign
```

### [OAuth 1.0](https://tools.ietf.org/html/rfc5849)

#### Token Credentials

```scala
  import io.bartholomews.fsclient.client.ClientData.sampleRedirectUri
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, SignatureMethod}
  import io.bartholomews.fsclient.core.oauth.v1.TemporaryCredentials
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.RedirectUri
  import io.bartholomews.fsclient.core.oauth.{
    RequestTokenCredentials,
    ResourceOwnerAuthorizationUri,
    TemporaryCredentialsRequest
  }
  import sttp.client3.{
    emptyRequest,
    HttpURLConnectionBackend,
    Identity,
    Response,
    ResponseException,
    SttpBackend,
    UriContext
  }
  import sttp.model.Method

  // Choose your effect / sttp backend
  type F[X] = Identity[X]

  val backend: SttpBackend[F, Any] = HttpURLConnectionBackend()

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
    )(backend)

  // a successful `resourceOwnerAuthorizationUriResponse` will have the token in the query parameters:
  val resourceOwnerAuthorizationUriResponse =
    sampleRedirectUri.value.withParams(("oauth_token", "AAA"), ("oauth_verifier", "ZZZ"))

  // 3. Extract the Token Credentials
  val maybeRequestTokenCredentials: Either[ResponseException[String, Exception], RequestTokenCredentials] =
    maybeTemporaryCredentials.body.flatMap { temporaryCredentials =>
      RequestTokenCredentials.fetchRequestTokenCredentials(
        resourceOwnerAuthorizationUriResponse,
        temporaryCredentials,
        SignatureMethod.PLAINTEXT
      )
    }

  maybeRequestTokenCredentials.map { implicit token =>
    // import `FsClientSttpExtensions` in http package to use `sign`
    import io.bartholomews.fsclient.core._

    // 4. Use the Token Credentials
    emptyRequest
      .get(uri"https://some-server/authenticated-endpoint")
      .sign // sign with the implicit token provided
  }
```

### [OAuth 2.0](https://tools.ietf.org/html/rfc6749)

#### Client credentials

```scala
  import io.bartholomews.fsclient.core.oauth.NonRefreshableTokenSigner
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ClientCredentialsGrant
  import io.bartholomews.fsclient.core.oauth.v2.{ClientId, ClientPassword, ClientSecret}
  import io.circe
  import sttp.client3.{HttpURLConnectionBackend, Identity, Response, ResponseException, SttpBackend, UriContext}

  type F[X] = Identity[X]

  val backend: SttpBackend[F, Any] = HttpURLConnectionBackend()

  // using fsclient-circe codecs, you could also use play-json or provide your own
  import io.bartholomews.fsclient.circe.codecs._

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
```

#### Implicit grant

```scala
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
```

#### Authorization code grant

```scala
  import io.bartholomews.fsclient.core._
  import io.bartholomews.fsclient.core.config.UserAgent
  import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AuthorizationCodeGrant, RedirectUri, RefreshToken}
  import io.bartholomews.fsclient.core.oauth.v2._
  import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, ClientPasswordAuthentication}
  import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend, UriContext}
  import sttp.model.Uri

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
  val maybeToken: Either[String, AccessTokenSigner] =
    maybeAuthorizationCode.flatMap { authorizationCode =>
      backend
        .send(
          AuthorizationCodeGrant
            .accessTokenRequest(
              serverUri = uri"https://some-authorization-server/token",
              code = authorizationCode,
              maybeRedirectUri = Some(myRedirectUri),
              clientPassword = myClientPassword
            )
        )
        .body
        .left
        .map(_.getMessage)
    }

  maybeToken.map { accessTokenSigner =>
    // 5. Use the access token
    baseRequest(userAgent)
      .get(uri"https://some-server/authenticated-endpoint")
      .sign(accessTokenSigner) // sign with the token signer

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