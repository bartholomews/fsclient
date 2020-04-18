[![CircleCI](https://circleci.com/gh/bartholomews/fsclient/tree/master.svg?style=svg)](https://circleci.com/gh/bartholomews/fsclient/tree/master)
[![codecov](https://codecov.io/gh/bartholomews/fsclient/branch/master/graph/badge.svg)](https://codecov.io/gh/bartholomews/fsclient)
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

# fsclient

🔧 **This project is still early stage and very much WIP / experimental** 🔧  

```
resolvers += "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
libraryDependencies += "io.bartholomews" %% "fsclient" % "0.0.1-SNAPSHOT"
```

*Opinionated* http client on top of http4s/fs2

Motivation for this project is to 
- play around with the Typelevel stack
- set up oAuth handling, logging, codecs patterns for api clients

```
import cats.effect.{ContextShift, IO}
import io.bartholomews.fsclient.client.FsClientV1
import io.bartholomews.fsclient.codecs.FsJsonResponsePipe
import io.bartholomews.fsclient.config.UserAgent
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.entities.oauth.OAuthVersion.OAuthV1
import io.bartholomews.fsclient.entities.oauth.{ClientCredentials, Signer}
import io.bartholomews.fsclient.requests.{FsSimpleRequest, JsonRequest}
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.{Decoder, Json}
import org.http4s.Uri

import scala.concurrent.ExecutionContext

object ReadmeTest extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  // will add header `User-Agent: myapp/0.0.1-SNAPSHOT (+com.github.bartholomews)` to all requests
  val userAgent = UserAgent(
    appName = "myapp",
    appVersion = Some("0.0.1-SNAPSHOT"),
    appUrl = Some("com.github.bartholomews")
  )

  val consumer = org.http4s.client.oauth1.Consumer(
    key = "kasldklSAJSALKDKAsd",
    secret = "asjdoASIDJASOdjasojdoasijd"
  )

  // Sign with consumer key/secret, but without token
  // Otherwise you can use `AuthVersion.V1.OAuthToken`
  val signer: Signer[OAuthV1] = ClientCredentials(consumer)

  // Define your expected response entity
  case class Todo(userId: Long, id: Long, title: String, completed: Boolean)
  // `FsJsonResponsePipe` is necessary to derive the fs2 Pipe decoder
  object Todo extends FsJsonResponsePipe[Todo] {
    implicit val decoder: Decoder[Todo] = io.circe.generic.semiauto.deriveDecoder
  }

  // You also need this for common codecs like empty body encoder and raw json decoder
  import io.bartholomews.fsclient.implicits._

  /*
    `FsSimpleRequest` has three type parameters:
      Body: the request body
      Raw: the raw response content-type
      Res: the decoded response into your own type
    Depending on the types you will be forced to add the request body
    and have the right implicits in scope for the codecs
   */
  val req: FsSimpleRequest[Nothing, Json, Todo] = new JsonRequest.Get[Todo] {
    override val uri: Uri = org.http4s.Uri.unsafeFromString("http://jsonplaceholder.typicode.com/todos/1")
  }

  // Run your `FsSimpleRequest` with the client for your effect
  val res: IO[HttpResponse[Todo]] = req.runWith(FsClientV1[IO, OAuthV1](userAgent, signer))

  res.unsafeRunSync() match {
    case FsResponseSuccess(_, _, todo) => println(todo.title)
    case res: FsResponseError[_]       => println(s"x--(ツ)--x => ${res.error}")
  }
}
```

## CircleCI deployment

### Verify local configuration
https://circleci.com/docs/2.0/local-cli/
```bash
circleci local execute
```

### Deploy to Sonatype

Follow the instructions [here](https://discuss.circleci.com/t/gpg-keys-as-environment-variables/28641/4) 
to setup the gpg private key to a CI machine (required for publishing to Sonatype)

### TODO
- [scala-steward](https://github.com/fthomas/scala-steward)