package io.bartholomews.fsclient

import cats.effect.{ContextShift, IO}
import io.bartholomews.fsclient.client.FsClientV1
import io.bartholomews.fsclient.codecs.FsJsonResponsePipe
import io.bartholomews.fsclient.config.UserAgent
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.entities.oauth.ClientCredentials
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
  val signer = ClientCredentials(consumer)

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
  val res: IO[HttpResponse[Todo]] = req.runWith(FsClientV1(userAgent, signer))

  val response = res.unsafeRunSync()

  println(response.headers)
  println(response.status)
  response.foldBody({
    case ErrorBodyString(error) => println(error)
    case ErrorBodyJson(error)   => println(error.spaces2)
  }, todo => println(todo.title))
}
