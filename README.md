[![CircleCI](https://circleci.com/gh/bartholomews/fsclient/tree/master.svg?style=svg)](https://circleci.com/gh/bartholomews/fsclient/tree/master)
[![codecov](https://codecov.io/gh/bartholomews/fsclient/branch/master/graph/badge.svg)](https://codecov.io/gh/bartholomews/fsclient)

<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

# fsclient
Opinionated http client on top of http4s

- OAuth
- Logging
- `Json` and `PlainText` http requests handling

```
import cats.effect.IO
import fsclient.config.AppConsumer
import fsclient.entities.{FsClientPlainRequest, HttpResponse, ResponseError}
import fsclient.http.client.IOSimpleClient
import org.http4s.Uri

import scala.concurrent.ExecutionContext

object TestMain extends App {

  // add `<logger name="fsclient-logger" level="DEBUG" />` to your `logback.xml`

  val client = new IOSimpleClient(
    // will add header `User-Agent: myapp/0.0.1-SNAPSHOT (+com.github.bartholomews)` to all requests
    AppConsumer(
      appName = "myapp",
      appVersion = Some("0.0.1-SNAPSHOT"),
      appUrl = Some("com.github.bartholomews"),
      key = "kasldklSAJSALKDKAsd",
      secret = "asjdoASIDJASOdjasojdoasijd"
    )
  )(ExecutionContext.global) // don't use this

  case class Todo(userId: Long, id: Long, title: String, completed: Boolean)
  object Todo {
    import io.circe.Decoder
    import io.circe.generic.semiauto.deriveDecoder
    implicit val decoder: Decoder[Todo] = deriveDecoder[Todo]
  }

  // http response with response `Headers` data and decoded entity
  val res: IO[HttpResponse[Todo]] = client.fetchJson[Todo](new FsClientPlainRequest.Get {
    override val uri: Uri = Uri.unsafeFromString("https://jsonplaceholder.typicode.com/todos/1")
  })

  val entity: Either[ResponseError, Todo] = res.map(_.entity).unsafeRunSync()
}
```

## Local development

### Run CircleCI locally
https://circleci.com/docs/2.0/local-cli/
```bash
circleci local execute
```

### TODO
- [scala-steward](https://github.com/fthomas/scala-steward)