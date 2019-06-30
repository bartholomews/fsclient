package fsclient.http.client

import cats.effect.IO
import cats.implicits._
import fsclient.entities.{HttpEndpoint, ResponseError}
import fsclient.mocks.server.WiremockServer
import fsclient.utils.HttpTypes
import io.circe.Json
import io.circe.syntax._

import org.http4s.Status
import org.scalatest.{Matchers, WordSpec}

class IOClientTest extends WordSpec with Matchers with WiremockServer with HttpTypes {

  "A valid simple client" when {

    val client = validSimpleClient

    def validPlainTextResponseGetEndpoint[E]: HttpEndpoint[E] = getEndpoint(okPlainTextResponse)
    def validPlainTextResponsePostEndpoint[E]: HttpEndpoint[E] = postEndpoint(okPlainTextResponse)
    def timeoutResponseGetEndpoint[E]: HttpEndpoint[E] = getEndpoint(timeoutResponse)
    def timeoutResponsePostEndpoint[E]: HttpEndpoint[E] = postEndpoint(timeoutResponse)

    import io.circe.generic.auto._
    case class MyRequestBody(a: String, b: List[Int])
    def requestBody: MyRequestBody = MyRequestBody("A", List(1, 2, 3))

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with Json response" when {

      "response is 200" should {

        def validResponseGetEndpoint[E]: HttpEndpoint[E] = getEndpoint(okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          val res = client.decodeJsonAs[Json](validResponseGetEndpoint).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          val res = client.decodeJsonAs(validResponseGetEndpoint[ValidEntity]).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(ValidEntity("this is a json response"))
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          val res = client.decodeJsonAs(validResponseGetEndpoint[InvalidEntity]).unsafeRunSync()
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
          val res = client.decodeJsonAs(validResponseGetEndpoint[InvalidEntity]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }
      }

      "response is 404" should {

        def notFoundJsonResponseGetEndpoint: HttpEndpoint[Json] = getEndpoint(notFoundJsonResponse)

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          val res = client.decodeJsonAs(notFoundJsonResponseGetEndpoint).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(e => e.getMessage) shouldBe Left(
            Map("message" -> "The requested resource was not found.").asJson.spaces2
          )
        }
      }

      "response is empty" should {

        def notFoundEmptyJsonResponseGetEndpoint: HttpEndpoint[Json] = getEndpoint(notFoundEmptyJsonBodyResponse)

        "respond with error for http response timeout" in {
          val res = client.decodeJsonAs(timeoutResponseGetEndpoint[String]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem with the response. Please check error logs"
          })
        }

        "return error with response status and default message" in {
          val res = client.decodeJsonAs(notFoundEmptyJsonResponseGetEndpoint).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("Response was empty. Please check request logs")
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodeJsonAs(getEndpoint[Json](badRequestNoContentTypeJsonResponse)).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("`Content-Type` not provided")
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodeJsonAs(getEndpoint[Json](badRequestWrongContentTypeJsonResponse)).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("multipart/form-data: unexpected `Content-Type`")
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          val res = client.decodePlainTextAs(validPlainTextResponseGetEndpoint[String]).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right("This is a valid plaintext response")
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] = _
            .map(e => MyEntity(e.toOption).asRight[ResponseError])

          val res1 = client.decodePlainTextAs(validPlainTextResponseGetEndpoint[MyEntity]).unsafeRunSync()
          res1.status shouldBe Status.Ok
          res1.entity shouldBe Right(MyEntity(Some("This is a valid plaintext response")))
        }

        "respond with empty string if the response body is empty" in {
          val res = client.decodePlainTextAs(getEndpoint[String](okEmptyPlainTextResponse)).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right("")
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          val res = client.decodeJsonAs(getEndpoint[String](notFoundPlainTextResponse)).unsafeRunSync()
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

          val res = client.decodePlainTextAs(getEndpoint[MyEntity](notFoundPlainTextResponse)).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(MyEntity(None))
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" in {
          val res = client.decodeJsonAs(timeoutResponseGetEndpoint[String]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem with the response. Please check error logs"
          })
        }

        "return error with response status and empty message" in {
          val res = client.decodeJsonAs(getEndpoint[String](notFoundEmptyPlainTextBodyResponse)).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("")
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodePlainTextAs(getEndpoint[String](badRequestNoContentTypeJsonResponse)).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("`Content-Type` not provided")
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodePlainTextAs(getEndpoint[String](badRequestWrongContentTypeJsonResponse)).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("multipart/form-data: unexpected `Content-Type`")
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with entity body and json response" when {

      "response is 200" should {

        def validResponsePostEndpoint[E]: HttpEndpoint[E] = postEndpoint(okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          val res = client.decodeJsonAs[Json, MyRequestBody](validResponsePostEndpoint, requestBody).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
        }

        "retrieve the decoded json with Status Ok and entity" in {
          case class ValidEntity(message: String)
          val res = client.decodeJsonAs(validResponsePostEndpoint[ValidEntity], requestBody).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(ValidEntity("this is a json response"))
        }

        "respond with error if the response json is unexpected" in {
          case class InvalidEntity(something: Boolean)
          val res = client.decodeJsonAs(validResponsePostEndpoint[InvalidEntity], requestBody).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }

        "respond with error if the response body is empty" in {
          case class InvalidEntity(something: Boolean)
          val res = client.decodeJsonAs(validResponsePostEndpoint[InvalidEntity], requestBody).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }
      }

      "response is 404" should {

        def notFoundJsonResponsePostEndpoint: HttpEndpoint[Json] = postEndpoint(notFoundJsonResponse)

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          val res = client.decodeJsonAs(notFoundJsonResponsePostEndpoint, requestBody).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(e => e.getMessage) shouldBe Left(
            Map("message" -> "The requested resource was not found.").asJson.spaces2
          )
        }
      }

      "response is empty" should {

        def notFoundEmptyJsonResponsePostEndpoint: HttpEndpoint[Json] = postEndpoint(notFoundEmptyJsonBodyResponse)

        "respond with error for http response timeout" in {
          val res = client.decodeJsonAs(timeoutResponsePostEndpoint[String], requestBody).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem with the response. Please check error logs"
          })
        }

        "return error with response status and default message" in {
          val res = client.decodeJsonAs(notFoundEmptyJsonResponsePostEndpoint, requestBody).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("Response was empty. Please check request logs")
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodeJsonAs(postEndpoint[Json](badRequestNoContentTypeJsonResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("`Content-Type` not provided")
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodeJsonAs(postEndpoint[Json](badRequestWrongContentTypeJsonResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("multipart/form-data: unexpected `Content-Type`")
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          val res = client.decodePlainTextAs(validPlainTextResponsePostEndpoint[String], requestBody).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right("This is a valid plaintext response")
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] = _
            .map(e => MyEntity(e.toOption).asRight[ResponseError])

          val res1 = client.decodePlainTextAs(validPlainTextResponsePostEndpoint[MyEntity], requestBody).unsafeRunSync()
          res1.status shouldBe Status.Ok
          res1.entity shouldBe Right(MyEntity(Some("This is a valid plaintext response")))
        }

        "respond with empty string if the response body is empty" in {
          val res = client.decodePlainTextAs(postEndpoint[String](okEmptyPlainTextResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right("")
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          val res = client.decodeJsonAs(postEndpoint[String](notFoundPlainTextResponse), requestBody).unsafeRunSync()
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

          val res = client.decodePlainTextAs(postEndpoint[MyEntity](notFoundPlainTextResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(MyEntity(None))
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" in {
          val res = client.decodeJsonAs(timeoutResponsePostEndpoint[String], requestBody).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem with the response. Please check error logs"
          })
        }

        "return error with response status and empty message" in {
          val res = client.decodeJsonAs(postEndpoint[String](notFoundEmptyPlainTextBodyResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("")
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodePlainTextAs(postEndpoint[String](badRequestNoContentTypeJsonResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("`Content-Type` not provided")
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          val res = client.decodePlainTextAs(postEndpoint[String](badRequestWrongContentTypeJsonResponse), requestBody).unsafeRunSync()
          res.status shouldBe Status.UnsupportedMediaType
          res.entity.leftMap(e => e.getMessage) shouldBe Left("multipart/form-data: unexpected `Content-Type`")
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with no entity body and json response" in {
      pending
    }
  }
}
