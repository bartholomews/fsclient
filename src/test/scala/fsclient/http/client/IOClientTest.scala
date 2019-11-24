package fsclient.http.client

import cats.effect.IO
import cats.implicits._
import fsclient.entities._
import fsclient.mocks.server.{OAuthServer, WiremockServer}
import fsclient.oauth.OAuthVersion
import fsclient.utils.HttpTypes.HttpPipe
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.Status
import org.scalatest.WordSpec
import org.scalatest.tagobjects.Slow

class IOClientTest extends WordSpec with IOClientMatchers with WiremockServer with OAuthServer {

  "A valid simple client with OAuth V1" when {

    val client = validSimpleClient(OAuthVersion.OAuthV1).simple

    def validPlainTextResponseGetEndpoint[R]: FsClientPlainRequest.Get =
      getEndpoint(okPlainTextResponse)

    def validPlainTextResponsePostEndpoint[B](body: B): FsClientRequestWithBody[B] =
      postJsonEndpoint(okPlainTextResponse, body)

    def timeoutResponseGetEndpoint[R]: FsClientPlainRequest.Get =
      getEndpoint(timeoutResponse)

    def timeoutResponsePostEndpoint[B](body: B): FsClientRequestWithBody[B] =
      postJsonEndpoint(timeoutResponse, body)

    case class MyRequestBody(a: String, b: List[Int])
    object MyRequestBody extends JsonRequest {
      import io.circe.generic.semiauto._
      implicit val encoder: Encoder[MyRequestBody] = deriveEncoder
    }

    val requestBody: MyRequestBody = MyRequestBody("A", List(1, 2, 3))

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with Json response" when {

      "response is 200" should {

        def validResponseGetEndpoint[R]: FsClientPlainRequest.Get =
          getEndpoint(okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            client.fetchJson[Json](validResponseGetEndpoint)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          assertRight(ValidEntity("this is a json response")) {
            client.fetchJson[ValidEntity](validResponseGetEndpoint)
          }
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.fetchJson[InvalidEntity](validResponseGetEndpoint)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.fetchJson[InvalidEntity](validResponseGetEndpoint[InvalidEntity])
          }
        }
      }

      "response is 404" should {
        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            client.fetchJson[Json](getEndpoint(notFoundJsonResponse))
          }
        }
      }

      "response is empty" should {

        def notFoundEmptyJsonResponseGetEndpoint: FsClientPlainRequest.Get =
          getEndpoint(notFoundEmptyJsonBodyResponse)

        "respond with error for http response timeout" taggedAs Slow in {
          assert500 {
            client.fetchJson[Json](timeoutResponseGetEndpoint[Json])
          }
        }

        "return error with response status and default message" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.emptyResponse) {
            client.fetchJson[Json](notFoundEmptyJsonResponseGetEndpoint)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME: expectedErrorMessage
            client.fetchJson[Json](getEndpoint(badRequestNoContentTypeNorBodyJsonResponse))
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "response=true&urlencoded=example") {
            client.fetchJson[Json](getEndpoint(badRequestMultipartJsonResponse))
          }
        }
      }
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          assertRight(expectedEntity = "This is a valid plaintext response") {
            client.fetchPlainText(validPlainTextResponseGetEndpoint[String])
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(expectedEntity = MyEntity(Some("This is a valid plaintext response"))) {
            client.fetchPlainText(validPlainTextResponseGetEndpoint[MyEntity])
          }
        }

        "respond with empty string if the response body is empty" in {
          assertRight(expectedEntity = "") {
            client.fetchPlainText(getEndpoint(okEmptyPlainTextResponse))
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            client.fetchJson[Json](getEndpoint(notFoundPlainTextResponse))
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(None)) {
            client.fetchPlainText[MyEntity](getEndpoint(notFoundPlainTextResponse))
          }
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" taggedAs Slow in {
          assert500 {
            client.fetchJson[Json](timeoutResponseGetEndpoint)
          }
        }

        "return error with response status and empty message" in {
          assertLeft(Status.NotFound, expectedErrorMessage = "") {
            client.fetchJson[Json](getEndpoint(notFoundEmptyPlainTextBodyResponse))
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME: expectedErrorMessage
            client.fetchPlainText[String](getEndpoint(badRequestNoContentTypeNorBodyJsonResponse))
          }
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with entity body and json response" when {

      "response is 200" should {

        def validResponsePostJsonEndpoint: FsClientRequestWithBody[MyRequestBody] =
          postJsonEndpoint(okJsonResponse, requestBody)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            client.fetchJsonWithBody[MyRequestBody, Json](validResponsePostJsonEndpoint)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          assertRight(ValidEntity("this is a json response")) {
            client.fetchJsonWithBody[MyRequestBody, ValidEntity](validResponsePostJsonEndpoint)
          }
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.fetchJsonWithBody[MyRequestBody, InvalidEntity](validResponsePostJsonEndpoint)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.fetchJsonWithBody[MyRequestBody, InvalidEntity](validResponsePostJsonEndpoint)
          }
        }
      }

      "response is 404" should {
        def notFoundJsonResponsePostEndpoint: FsClientRequestWithBody[MyRequestBody] =
          postJsonEndpoint(notFoundJsonResponse, requestBody)

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            client.fetchJsonWithBody[MyRequestBody, Json](notFoundJsonResponsePostEndpoint)
          }
        }
      }

      "response is empty" should {
        def notFoundEmptyJsonResponsePostEndpoint: FsClientRequestWithBody[MyRequestBody] =
          postJsonEndpoint(notFoundEmptyJsonBodyResponse, requestBody)

        "respond with error for http response timeout" taggedAs Slow in {
          assert500 {
            client.fetchJsonWithBody[MyRequestBody, Json](timeoutResponsePostEndpoint(requestBody))
          }
        }

        "return error with response status and default message" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.emptyResponse) {
            client.fetchJsonWithBody[MyRequestBody, Json](notFoundEmptyJsonResponsePostEndpoint)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME: expectedErrorMessage
            client.fetchJsonWithBody[MyRequestBody, Json](
              postJsonEndpoint(badRequestNoContentTypeNorBodyJsonResponse, requestBody)
            )
          }
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          assertRight("This is a valid plaintext response") {
            client
              .fetchPlainTextWithBody {
                validPlainTextResponsePostEndpoint[MyRequestBody](requestBody)
              }
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(Some("This is a valid plaintext response"))) {
            client
              .fetchPlainTextWithBody[MyRequestBody, MyEntity] {
                validPlainTextResponsePostEndpoint(requestBody)
              }
          }
        }

        "respond with empty string if the response body is empty" in {
          assertRight(expectedEntity = "") {
            client
              .fetchPlainTextWithBody {
                postJsonEndpoint[MyRequestBody](okEmptyPlainTextResponse, requestBody)
              }
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            client.fetchJsonWithBody[MyRequestBody, Json] {
              postJsonEndpoint(notFoundPlainTextResponse, requestBody)
            }
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(None)) {
            client
              .fetchPlainTextWithBody {
                postJsonEndpoint[MyRequestBody](notFoundPlainTextResponse, requestBody)
              }
          }
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" taggedAs Slow in {
          assert500 {
            client.fetchJsonWithBody[MyRequestBody, Json](timeoutResponsePostEndpoint(requestBody))
          }
        }

        "return error with response status and empty message" in {
          assertLeft(Status.NotFound, expectedErrorMessage = "") {
            client
              .fetchJsonWithBody[MyRequestBody, Json](
                postJsonEndpoint(notFoundEmptyPlainTextBodyResponse, requestBody)
              )
          }
        }
      }

      "response has no `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME expectedErrorMessage
            client
              .fetchPlainTextWithBody[MyRequestBody, String](
                postJsonEndpoint(badRequestNoContentTypeNorBodyJsonResponse, requestBody)
              )
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "response=true&urlencoded=example") {
            client
              .fetchPlainTextWithBody[MyRequestBody, String](
                postJsonEndpoint(badRequestMultipartJsonResponse, requestBody)
              )
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
                    Left(ResponseError(new Exception(s"Unexpected response:\n[$invalid]"), Status.UnsupportedMediaType))
                }
            )
          )

          val res =
            validSimpleClient(OAuthVersion.OAuthV1).auth.toOAuthClient(validAccessTokenEndpoint).unsafeRunSync()
          inside(res) {
            case Right(oAuthClient) =>
              oAuthClient.consumer shouldBe client.consumer
          }
        }
      }
    }
  }
}

// TODO: test `oAuthClient` calls with valid and invalid token
// TODO: test `simpleClient.auth` calls with valid and invalid token
