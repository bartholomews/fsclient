package fsclient.http.client

import cats.effect.IO
import cats.implicits._
import fsclient.entities.{AccessToken, HttpRequest, HttpResponse, ResponseError}
import fsclient.mocks.server.{OAuthServer, WiremockServer}
import fsclient.utils.HttpTypes
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.{Assertion, Inside, Matchers, WordSpec}

class IOClientTest
    extends WordSpec
    with Matchers
    with Inside
    with WiremockServer
    with HttpTypes
    with OAuthServer {

  "A valid simple client" when {

    val client = validSimpleClient

    def validPlainTextResponseGetEndpoint[R]: HttpRequest.GET[R] =
      getEndpoint[R](okPlainTextResponse)

    def validPlainTextResponsePostEndpoint[B, R](body: B): HttpRequest[B, R] =
      postEndpoint(okPlainTextResponse, body)

    def timeoutResponseGetEndpoint[R]: HttpRequest.GET[R] =
      getEndpoint(timeoutResponse)

    def timeoutResponsePostEndpoint[B, R](body: B): HttpRequest[B, R] =
      postEndpoint(timeoutResponse, body)

    import io.circe.generic.auto._
    case class MyRequestBody(a: String, b: List[Int])
    val requestBody: MyRequestBody = MyRequestBody("A", List(1, 2, 3))

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with Json response" when {

      def insideResponse[R](ioResponse: IOResponse[R])(
          f: HttpResponse[R] => Assertion) =
        f(ioResponse.unsafeRunSync())

      "response is 200" should {

        def validResponseGetEndpoint[R] = getEndpoint[R](okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          insideResponse(client.getJson[Json](None)(validResponseGetEndpoint)) {
            case response @ HttpResponse(_, entity) =>
              response.status shouldBe Status.Ok
              entity shouldBe Right(
                Map("message" -> "this is a json response").asJson
              )
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          val res = client
            .getJson[ValidEntity](accessToken = None)(validResponseGetEndpoint)
            .unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(ValidEntity("this is a json response"))
        }

        "respond with error if the response json is unexpected" in {
          case class InvalidEntity(something: Boolean)
          val res = client
            .getJson[InvalidEntity](accessToken = None)(
              validResponseGetEndpoint)
            .unsafeRunSync()

          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }

        //        "respond with error if the response body is empty" in {
        //                    import io.circe.generic.auto._
        //          case class InvalidEntity(something: Boolean)
        //          val res = client.fetchJson(validResponseGetEndpoint[InvalidEntity]).unsafeRunSync()
        //          res.status shouldBe Status.InternalServerError
        //          res.entity shouldBe a[Left[_, _]]
        //          res.entity.leftMap(err => {
        //            err.status shouldBe Status.InternalServerError
        //            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
        //          })
        //        }
        //      }

        "response is 404" should {

          def notFoundJsonResponseGetEndpoint: HttpRequest.GET[Json] =
            getEndpoint(notFoundJsonResponse)

          "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
            val res = client
              .getJson(accessToken = None)(notFoundJsonResponseGetEndpoint)
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              Map("message" -> "The requested resource was not found.").asJson.spaces2
            )
          }
        }

        "response is empty" should {

          def notFoundEmptyJsonResponseGetEndpoint: HttpRequest.GET[Json] =
            getEndpoint(notFoundEmptyJsonBodyResponse)

          "respond with error for http response timeout" in {
            val res = client
              .getJson[Json](accessToken = None)(
                timeoutResponseGetEndpoint[Json])
              .unsafeRunSync()
            res.status shouldBe Status.InternalServerError
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(err => {
              err.status shouldBe Status.InternalServerError
              err.getMessage shouldBe "There was a problem with the response. Please check error logs"
            })
          }

          "return error with response status and default message" in {
            val res = client
              .getJson(accessToken = None)(notFoundEmptyJsonResponseGetEndpoint)
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "Response was empty. Please check request logs")
          }
        }

        "response has no `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .getJson(accessToken = None)(
                getEndpoint[Json](badRequestNoContentTypeJsonResponse))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "`Content-Type` not provided")
          }
        }

        "response has an unexpected `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .getJson(accessToken = None)(
                getEndpoint[Json](badRequestWrongContentTypeJsonResponse))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "multipart/form-data: unexpected `Content-Type`")
          }
        }
      }

      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

      "calling a GET endpoint with plainText response" when {

        "response is 200" should {

          "retrieve the response with Status Ok and string entity" in {
            val res = client
              .getPlainText(accessToken = None)(
                validPlainTextResponseGetEndpoint[String])
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right("This is a valid plaintext response")
          }

          "respond applying the provided `plainTextDecoder`" in {
            case class MyEntity(str: Option[String])

            implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
              _.map(e => MyEntity(e.toOption).asRight[ResponseError])

            val res1 = client
              .getPlainText(accessToken = None)(
                validPlainTextResponseGetEndpoint[MyEntity])
              .unsafeRunSync()
            res1.status shouldBe Status.Ok
            res1.entity shouldBe Right(
              MyEntity(Some("This is a valid plaintext response")))
          }

          "respond with empty string if the response body is empty" in {
            val res = client
              .getPlainText(accessToken = None)(
                getEndpoint[String](okEmptyPlainTextResponse))
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right("")
          }
        }

        "response is 404" should {

          "retrieve the string response with Status NotFound" in {
            val res = client
              .getJson(accessToken = None)(
                getEndpoint[String](notFoundPlainTextResponse))
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "The requested resource was not found."
            )
          }

          "respond applying the provided `plainTextDecoder`" in {
            case class MyEntity(str: Option[String])

            implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
              _.map(e => MyEntity(e.toOption).asRight[ResponseError])

            val res = client
              .getPlainText(accessToken = None)(
                getEndpoint[MyEntity](notFoundPlainTextResponse))
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right(MyEntity(None))
          }
        }

        "response is empty" should {

          "respond with error for http response timeout" in {
            val res = client
              .getJson[Json](accessToken = None)(timeoutResponseGetEndpoint)
              .unsafeRunSync()
            res.status shouldBe Status.InternalServerError
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(err => {
              err.status shouldBe Status.InternalServerError
              err.getMessage shouldBe "There was a problem with the response. Please check error logs"
            })
          }

          "return error with response status and empty message" in {
            val res = client
              .getJson(accessToken = None)(
                getEndpoint[String](notFoundEmptyPlainTextBodyResponse))
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity.leftMap(e => e.getMessage) shouldBe Left("")
          }
        }

        "response has no `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .getPlainText(accessToken = None)(
                getEndpoint[String](badRequestNoContentTypeJsonResponse))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "`Content-Type` not provided")
          }
        }

        "response has an unexpected `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .getPlainText(accessToken = None)(
                getEndpoint[String](badRequestWrongContentTypeJsonResponse))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "multipart/form-data: unexpected `Content-Type`")
          }
        }
      }

      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

      "calling a POST endpoint with entity body and json response" when {

        "response is 200" should {

          def validResponsePostEndpoint[R]: HttpRequest[MyRequestBody, R] =
            postEndpoint(okJsonResponse, requestBody)

          "retrieve the json with Status Ok and entity" in {
            val res = client
              .fetchJson[MyRequestBody, Json](accessToken = None)(
                validResponsePostEndpoint) // FIXME [FB] remove types and see
              .unsafeRunSync()

            res.status shouldBe Status.Ok
            res.entity shouldBe Right(
              Map("message" -> "this is a json response").asJson)
          }

          "retrieve the decoded json with Status Ok and entity" in {
            case class ValidEntity(message: String)
            val res = client
              .fetchJson(accessToken = None)(
                validResponsePostEndpoint[ValidEntity])
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right(ValidEntity("this is a json response"))
          }

          "respond with error if the response json is unexpected" in {
            case class InvalidEntity(something: Boolean)
            val res = client
              .fetchJson(accessToken = None)(
                validResponsePostEndpoint[InvalidEntity])
              .unsafeRunSync()
            res.status shouldBe Status.InternalServerError
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(err => {
              err.status shouldBe Status.InternalServerError
              err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
            })
          }

          "respond with error if the response body is empty" in {
            case class InvalidEntity(something: Boolean)
            val res = client
              .fetchJson(accessToken = None)(
                validResponsePostEndpoint[InvalidEntity])
              .unsafeRunSync()
            res.status shouldBe Status.InternalServerError
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(err => {
              err.status shouldBe Status.InternalServerError
              err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
            })
          }
        }

        "response is 404" should {

          def notFoundJsonResponsePostEndpoint
            : HttpRequest[MyRequestBody, Json] =
            postEndpoint(notFoundJsonResponse, requestBody)

          "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
            val res = client
              .fetchJson(accessToken = None)(notFoundJsonResponsePostEndpoint)
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              Map("message" -> "The requested resource was not found.").asJson.spaces2
            )
          }
        }

        "response is empty" should {

          def notFoundEmptyJsonResponsePostEndpoint
            : HttpRequest[MyRequestBody, Json] =
            postEndpoint(notFoundEmptyJsonBodyResponse, requestBody)

          "respond with error for http response timeout" in {
            val res = client
              .fetchJson[MyRequestBody, Json](accessToken = None)(
                timeoutResponsePostEndpoint(requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.InternalServerError
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(err => {
              err.status shouldBe Status.InternalServerError
              err.getMessage shouldBe "There was a problem with the response. Please check error logs"
            })
          }

          "return error with response status and default message" in {
            val res = client
              .fetchJson(accessToken = None)(
                notFoundEmptyJsonResponsePostEndpoint)
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "Response was empty. Please check request logs")
          }
        }

        "response has no `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .fetchJson[MyRequestBody, Json](accessToken = None)(
                postEndpoint(badRequestNoContentTypeJsonResponse, requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "`Content-Type` not provided")
          }
        }

        "response has an unexpected `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .fetchJson(accessToken = None)(
                postEndpoint[MyRequestBody, Json](
                  badRequestWrongContentTypeJsonResponse,
                  requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "multipart/form-data: unexpected `Content-Type`")
          }
        }
      }

      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

      "calling a POST endpoint with plainText response" when {

        "response is 200" should {

          "retrieve the response with Status Ok and string entity" in {
            val res = client
              .fetchPlainText(accessToken = None)(
                validPlainTextResponsePostEndpoint[MyRequestBody, String](
                  requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right("This is a valid plaintext response")
          }

          "respond applying the provided `plainTextDecoder`" in {
            case class MyEntity(str: Option[String])

            implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
              _.map(e => MyEntity(e.toOption).asRight[ResponseError])

            val res1 = client
              .fetchPlainText[MyRequestBody, MyEntity](accessToken = None)(
                validPlainTextResponsePostEndpoint(requestBody))
              .unsafeRunSync()

            res1.status shouldBe Status.Ok
            res1.entity shouldBe Right(
              MyEntity(Some("This is a valid plaintext response")))
          }

          "respond with empty string if the response body is empty" in {
            val res = client
              .fetchPlainText(accessToken = None)(
                postEndpoint[MyRequestBody, String](okEmptyPlainTextResponse,
                                                    requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right("")
          }
        }

        "response is 404" should {

          "retrieve the string response with Status NotFound" in {
            val res = client
              .fetchJson(accessToken = None)(
                postEndpoint[MyRequestBody, String](notFoundPlainTextResponse,
                                                    requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "The requested resource was not found."
            )
          }

          "respond applying the provided `plainTextDecoder`" in {
            case class MyEntity(str: Option[String])

            implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
              _.map(e => MyEntity(e.toOption).asRight[ResponseError])

            val res = client
              .fetchPlainText(accessToken = None)(
                postEndpoint[MyRequestBody, MyEntity](notFoundPlainTextResponse,
                                                      requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.Ok
            res.entity shouldBe Right(MyEntity(None))
          }
        }

        "response is empty" should {

          "respond with error for http response timeout" in {
            val res = client
              .fetchJson(accessToken = None)(
                timeoutResponsePostEndpoint[MyRequestBody, String](requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.InternalServerError
            res.entity shouldBe a[Left[_, _]]
            res.entity.leftMap(err => {
              err.status shouldBe Status.InternalServerError
              err.getMessage shouldBe "There was a problem with the response. Please check error logs"
            })
          }

          "return error with response status and empty message" in {
            val res = client
              .fetchJson(accessToken = None)(
                postEndpoint[MyRequestBody, String](
                  notFoundEmptyPlainTextBodyResponse,
                  requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.NotFound
            res.entity.leftMap(e => e.getMessage) shouldBe Left("")
          }
        }

        "response has no `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .fetchPlainText(accessToken = None)(
                postEndpoint[MyRequestBody, String](
                  badRequestNoContentTypeJsonResponse,
                  requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "`Content-Type` not provided")
          }
        }

        "response has an unexpected `Content-Type`" should {
          "return 415 with the right error" in {
            val res = client
              .fetchPlainText(accessToken = None)(
                postEndpoint[MyRequestBody, String](
                  badRequestWrongContentTypeJsonResponse,
                  requestBody))
              .unsafeRunSync()
            res.status shouldBe Status.UnsupportedMediaType
            res.entity.leftMap(e => e.getMessage) shouldBe Left(
              "multipart/form-data: unexpected `Content-Type`")
          }
        }
      }

      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

      "calling `accessTokenRequest`" should {
        "work" in {

          import org.http4s.client.oauth1.Token

          implicit val decoder: HttpPipe[IO, String, AccessToken] = _.map(
            _.fold(
              err => Left(err),
              str =>
                str match {
                  case accessTokenResponseRegex(tokenValue, tokenSecret) =>
                    Right(AccessToken(Token(tokenValue, tokenSecret)))
                  case invalid =>
                    Left(ResponseError(
                      new Exception(s"Unexpected response:\n[$invalid]")))
              }
            ))

          val res =
            client.toOAuthClient(validAccessTokenEndpoint).unsafeRunSync()
          inside(res) {
            case Right(oAuthClient) =>
              oAuthClient.consumer shouldBe client.consumer
          }
        }
      }
    }
  }
}

// TODO: test oAuth calls with valid and invalid token
