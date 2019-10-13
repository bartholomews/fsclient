[![CircleCI](https://circleci.com/gh/bartholomews/fsclient/tree/master.svg?style=svg)](https://circleci.com/gh/bartholomews/fsclient/tree/master)
[![codecov](https://codecov.io/gh/bartholomews/fsclient/branch/master/graph/badge.svg)](https://codecov.io/gh/bartholomews/fsclient)

<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

# fsclient
Opinionated http client on top of http4s

- OAuth
- Logging
- `Json` and `PlainText` http requests handling

```
import io.circe.generic.auto._

val client = new IOSimpleClient(
    OAuthConsumer(
        "MY_APP_NAME",
        appVersion = None,
        appUrl = None,
        "OAUTH_CONSUMER_KEY",
        "OAUTH_CONSUMER_SECRET"
    )
)

val getMyEntityEndpoint = new FsClientPlainRequest.GET[MyEntity] {
    override val uri: Uri = Uri.unsafeFromString("0.0.0.0/endpoint")
}

case class MyEntity(message: String)

client.getJson[ValidEntity](accessToken = None)(getMyEntityEndpoint)

```

## Local development

### Run CircleCI locally
https://circleci.com/docs/2.0/local-cli/
```bash
circleci local execute
```

### TODO
- [scala-steward](https://github.com/fthomas/scala-steward)