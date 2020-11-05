package io.bartholomews.fsclient

import cats.effect.{ContextShift, IO}
import io.bartholomews.fsclient.client.FsClientV1
import io.bartholomews.fsclient.config.UserAgent
import io.bartholomews.fsclient.entities._
import io.bartholomews.fsclient.entities.oauth.{ClientCredentials, SignerV1}
import io.bartholomews.fsclient.requests.{FsSimpleJson, FsSimpleRequest}
import io.bartholomews.fsclient.utils.HttpTypes.HttpResponse
import io.circe.{Codec, Json}
import org.http4s.Uri

import scala.concurrent.ExecutionContext

object Example extends App {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  // will add header `User-Agent: myapp/0.0.1-SNAPSHOT (+com.github.bartholomews)` to all requests
  val userAgent = UserAgent(
    appName = "myapp",
    appVersion = Some("0.0.1-SNAPSHOT"),
    appUrl = Some("com.github.bartholomews")
  )

  val consumer = org.http4s.client.oauth1.Consumer(
    key = "CONSUMER_KEY",
    secret = "CONSUMER_SECRET"
  )

  // Sign with consumer key/secret, but without token
  // Otherwise you can use `AuthVersion.V1.OAuthToken`
  val signer = ClientCredentials(consumer)

  // Define your expected response entity
  case class SampleEntity(userId: Long, id: Long, title: String, body: String)
  object SampleEntity {
    implicit val codec: Codec[SampleEntity] = io.circe.generic.semiauto.deriveCodec
  }

  val postsUri: Uri = org.http4s.Uri.unsafeFromString("http://jsonplaceholder.typicode.com/posts")

  /*
    `FsRequest` has three type parameters:
      Body: the request body
      Raw: the raw response content-type
      Res: the decoded response into your own type
     Depending on the types you will be forced to add the request body
     and have the right implicits in scope for the codecs.
    `FsSimple` requests will use the client signer,
    `FsAuth` requests require their own signer.
   */
  val getEntities: FsSimpleRequest[Nothing, Json, List[SampleEntity]] = new FsSimpleJson.Get[List[SampleEntity]] {
    override val uri: Uri = postsUri
  }

  val postEntity: FsSimpleRequest[SampleEntity, Json, Unit] =
    new FsSimpleJson.Post[SampleEntity, Unit] {
      override val uri: Uri = org.http4s.Uri.unsafeFromString("http://jsonplaceholder.typicode.com/posts")
      override def requestBody: SampleEntity = SampleEntity(userId = 1L, id = 1L, title = "A sample entity", body = "_")
    }

  // An OAuth v1 client with ClientCredentials signer and cats IO
  val client: FsClientV1[IO, SignerV1] = FsClientV1(userAgent, signer)

  // Run your request with the client for your effect
  val res: IO[HttpResponse[List[SampleEntity]]] = for {
    _ <- postEntity.runWith(client)
    maybeEntity <- getEntities.runWith(client)
  } yield maybeEntity

  val response = res.unsafeRunSync()
  println(response.headers)
  println(response.status)
  response.foldBody(
    {
      case ErrorBodyString(error) => println(error)
      case ErrorBodyJson(error)   => println(error.spaces2)
    },
    todo => println(todo.head.title)
  )
}
