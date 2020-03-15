[![CircleCI](https://circleci.com/gh/bartholomews/fsclient/tree/master.svg?style=svg)](https://circleci.com/gh/bartholomews/fsclient/tree/master)
[![codecov](https://codecov.io/gh/bartholomews/fsclient/branch/master/graph/badge.svg)](https://codecov.io/gh/bartholomews/fsclient)
[![Download](https://api.bintray.com/packages/bartholomews/maven/fsclient/images/download.svg) ](https://bintray.com/bartholomews/maven/fsclient/_latestVersion)   
<a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

# fsclient

🔧 **This project is still early stage and very much WIP / experimental** 🔧  

*Opinionated* http client on top of http4s/fs2

Motivation for this project is to 
- play around with the Typelevel stack
- set up oAuth handling, logging, codecs patterns for api clients

```
package fsclient

import cats.effect.IO
import fsclient.client.io_client.IOAuthClient
import fsclient.codecs.FsJsonResponsePipe
import fsclient.config.UserAgent
import fsclient.entities.{AuthVersion, HttpResponse, ResponseError, Signer}
import fsclient.requests.{FsSimpleRequest, JsonRequest}
import io.circe.{Decoder, Json}

import scala.concurrent.ExecutionContext

object SimpleRequestExample {

  implicit val ec: ExecutionContext = ExecutionContext.global

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
  val signer: Signer = AuthVersion.V1.BasicSignature(consumer)

  // Define your expected response entity
  case class Todo(userId: Long, id: Long, title: String, completed: Boolean)
  // `FsJsonResponsePipe` is necessary to derive the fs2 Pipe decoder
  object Todo extends FsJsonResponsePipe[Todo] {
    implicit val decoder: Decoder[Todo] = io.circe.generic.semiauto.deriveDecoder
  }

  // You also need this for common codecs like empty body encoder and raw json decoder
  import fsclient.implicits._

  /*
    `FsSimpleRequest` has three type parameters:
      Body: the request body
      Raw: the raw response content-type
      Res: the decoded response into your own type
    Depending on the types you will be forced to add the request body
    and have the right implicits in scope for the codecs
   */
  val req: FsSimpleRequest[Nothing, Json, Todo] = new JsonRequest.Get[Todo] {
    override val uri = org.http4s.Uri.unsafeFromString("http://jsonplaceholder.typicode.com/todos/1")
  }

  // Run your `FsSimpleRequest` with the client for your effect
  val res: IO[HttpResponse[Todo]] = req.runWith(new IOAuthClient(userAgent, signer))

  def main(args: Array[String]): Unit =
    res
      .unsafeRunSync()
      .entity match {
      case Left(responseError: ResponseError) => println(s"x--(ツ)--x => ${responseError.status}")
      case Right(todo: Todo)                  => println(todo.title)
    }
}
```

## Local development

### Run CircleCI locally
https://circleci.com/docs/2.0/local-cli/
```bash
circleci local execute
```

## Deployment

Follow the instructions [here](https://unix.stackexchange.com/a/482559) 
to pass the gpg private key to a CI machine (required for publishing to Sonatype)

### TODO
- [scala-steward](https://github.com/fthomas/scala-steward)