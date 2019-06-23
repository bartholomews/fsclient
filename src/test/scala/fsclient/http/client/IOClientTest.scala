package fsclient.http.client

import cats.effect.IO
import cats.implicits._
import fsclient.entities.{GET, HttpEndpoint, ResponseError}
import fsclient.mocks.server.WiremockServer
import fsclient.utils.HttpTypes
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.{Matchers, WordSpec}

// TODO IOWordSpec
class IOClientTest extends WordSpec with Matchers with WiremockServer with HttpTypes {

  "A valid simple client" when {

    val client = validSimpleClient

    def validPlainTextResponseGetEndpoint[E]: HttpEndpoint[E, GET] =
      validPlainTextResponseEndpoint[E, GET](GET())

    def timeoutResponseGetEndpoint[E]: HttpEndpoint[E, GET] =
      timeoutResponseEndpoint[E, GET](GET())

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with Json response" when {

      "response is 200" should {

        def validResponseGetEndpoint[E]: HttpEndpoint[E, GET] =
          validResponseEndpoint(GET())

        "retrieve the json with Status Ok and entity" in {
          val res = client.getAndDecodeJsonAs[Json](validResponseGetEndpoint).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          val res = client.getAndDecodeJsonAs(validResponseGetEndpoint[ValidEntity]).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(ValidEntity("this is a json response"))
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          val res = client.getAndDecodeJsonAs(validResponseGetEndpoint[InvalidEntity]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          val res = client.getAndDecodeJsonAs(validResponseGetEndpoint[InvalidEntity]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }
      }

      "response is 404" should {

        def notFoundJsonResponseGetEndpoint: HttpEndpoint[Json, GET] = notFoundJsonResponseEndpoint(GET())

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          val res = client.getAndDecodeJsonAs(notFoundJsonResponseGetEndpoint).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(e => e.getMessage) shouldBe Left(
            Map("message" -> "The requested resource was not found.").asJson.spaces2
          )
        }
      }

      "response is empty" should {

        def notFoundEmptyJsonResponseGetEndpoint: HttpEndpoint[Json, GET] = notFoundEmptyJsonResponseEndpoint(GET())

        "respond with error for http response timeout" in {
          val res = client.getAndDecodeJsonAs(timeoutResponseGetEndpoint[String]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem with the response. Please check error logs"
          })
        }

        "return error with response status and default message" in {
          val res = client.getAndDecodeJsonAs(notFoundEmptyJsonResponseGetEndpoint).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("Response was empty. Please check request logs")
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          val res = client.getAndDecodeJsonAs(validPlainTextResponseGetEndpoint[String]).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right("This is a valid plaintext response")
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] = _
            .map(e => MyEntity(e.toOption).asRight[ResponseError])

          val res1 = client.getAndDecodePlainTextAs(validPlainTextResponseGetEndpoint[MyEntity]).unsafeRunSync()
          res1.status shouldBe Status.Ok
          res1.entity shouldBe Right(MyEntity(Some("This is a valid plaintext response")))
        }

        "respond with empty string if the response body is empty" in {
          val res = client.getAndDecodeJsonAs(getEndpoint[String](okEmptyPlainTextResponse)).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right("")
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          val res = client.getAndDecodeJsonAs(getEndpoint[String](notFoundPlainTextResponse)).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(e => e.getMessage) shouldBe Left(
            "The requested resource was not found."
          )
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] = _
            .map(e => MyEntity(e.toOption).asRight[ResponseError])

          val res = client.getAndDecodePlainTextAs(getEndpoint[MyEntity](notFoundPlainTextResponse)).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(MyEntity(None))
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" in {
          val res = client.getAndDecodeJsonAs(timeoutResponseGetEndpoint[String]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem with the response. Please check error logs"
          })
        }

        "return error with response status and empty message" in {
          val res = client.getAndDecodeJsonAs(getEndpoint[String](notFoundEmptyPlainTextBodyResponse)).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("")
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with no entity body and json response" in {
      pending
    }

    "calling a POST endpoint with entity body and json response" in {
      pending
    }

    "calling a POST endpoint with plainText response" in {
      pending
    }
    //  private def POST[A](uri: Uri)
    //                     (implicit entityEncoder: EntityEncoder[F, Json]): Request[F] =
    //    request(uri).withMethod(Method.POST).withEntity[Json]("dsd".asJson)
    //  import org.http4s.circe.CirceEntityEncoder._
    //  import org.http4s.circe._
    //
    //  import org.http4s.circe.CirceEntityEncoder
    //  import org.http4s.circe.CirceEntityDecoder._
    //  import org.http4s.circe.CirceEntityCodec._
    //  import org.http4s.circe.CirceInstances._
    //
    //  import io.circe.generic.auto._

    //  implicit val body = jsonEncoderOf[F, Json]

    //  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A],
    //                                   applicative: Applicative[F]): EntityEncoder[F, A] =
    //    org.http4s.circe.jsonEncoderOf[F, A]
  }
}
