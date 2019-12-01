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
    // will add header `User-Agent: myapp/0.0.1-SNAPSHOT (+com.github.bartholomews)`
    OAuthConsumer(
        appName = "myapp",
        appVersion = Some("0.0.1-SNAPSHOT"),
        appUrl = Some("com.github.bartholomews"),
        key = "kasldklSAJSALKDKAsd",
        secret = "asjdoASIDJASOdjasojdoasijd"
    )
)

val getMyEntityEndpoint = new FsClientPlainRequest.Get {
    override val uri: Uri = Uri.unsafeFromString("0.0.0.0/endpoint")
}

case class MyEntity(message: String)
object MyEntity {
    import io.circe.Decoder
    import io.circe.generic.semiauto.deriveDecoder
    implicit val decoder: Decoder[MyEntity] = deriveDecoder[MyEntity]
}

val message = client
    .fetchJson[ValidEntity](getMyEntityEndpoint)
    .map(_.entity)
    .map(_.message)
    .unsafeRunSync()

val res: IO[HttpResponse[ValidEntity]] = client.fetchJson[ValidEntity](getMyEntityEndpoint)
val entity: IO[Either[ResponseError, ValidEntity]] = res.map(_.entity)

```

## Local development

### Run CircleCI locally
https://circleci.com/docs/2.0/local-cli/
```bash
circleci local execute
```

### TODO
- [scala-steward](https://github.com/fthomas/scala-steward)