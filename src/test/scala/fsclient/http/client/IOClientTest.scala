package fsclient.http.client

import cats.effect.IO
import cats.implicits._
import fsclient.entities.{AccessToken, FsClientPlainRequest, FsClientRequestWithBody, ResponseError}
import fsclient.mocks.server.{OAuthServer, WiremockServer}
import fsclient.utils.HttpTypes
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.WordSpec

class IOClientTest extends WordSpec with IOClientMatchers with WiremockServer with HttpTypes with OAuthServer {

  "A valid simple client" when {

    val client = validSimpleClient

    def validPlainTextResponseGetEndpoint[R]: FsClientPlainRequest.GET[R] =
      getEndpoint[R](okPlainTextResponse)

    def validPlainTextResponsePostEndpoint[B, R](body: B): FsClientRequestWithBody[B, R] =
      postEndpoint(okPlainTextResponse, body)

    def timeoutResponseGetEndpoint[R]: FsClientPlainRequest.GET[R] =
      getEndpoint(timeoutResponse)

    def timeoutResponsePostEndpoint[B, R](body: B): FsClientRequestWithBody[B, R] =
      postEndpoint(timeoutResponse, body)

    import io.circe.generic.auto._
    case class MyRequestBody(a: String, b: List[Int])
    val requestBody: MyRequestBody = MyRequestBody("A", List(1, 2, 3))

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with Json response" when {

      "response is 200" should {

        def validResponseGetEndpoint[R]: FsClientPlainRequest.GET[R] =
          getEndpoint[R](okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            client.getJson[Json](None)(validResponseGetEndpoint)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          assertRight(ValidEntity("this is a json response")) {
            client.getJson[ValidEntity](accessToken = None)(validResponseGetEndpoint)
          }
        }

        "respond with error if the response json is unexpected" in {
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.getJson[InvalidEntity](accessToken = None)(validResponseGetEndpoint)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.getJson[InvalidEntity](accessToken = None)(validResponseGetEndpoint[InvalidEntity])
          }
        }
      }

      "response is 404" should {
        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            client.getJson[Json](accessToken = None)(getEndpoint(notFoundJsonResponse))
          }
        }
      }

      "response is empty" should {

        def notFoundEmptyJsonResponseGetEndpoint: FsClientPlainRequest.GET[Json] =
          getEndpoint(notFoundEmptyJsonBodyResponse)

        "respond with error for http response timeout" in {
          assert500 {
            client.getJson[Json](accessToken = None)(timeoutResponseGetEndpoint[Json])
          }
        }

        "return error with response status and default message" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.emptyResponse) {
            client.getJson(accessToken = None)(notFoundEmptyJsonResponseGetEndpoint)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType, expectedErrorMessage = "`Content-Type` not provided") {
            client.getJson(accessToken = None)(getEndpoint[Json](badRequestNoContentTypeJsonResponse))
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType,
                     expectedErrorMessage = "multipart/form-data: unexpected `Content-Type`") {
            client.getJson(accessToken = None)(getEndpoint[Json](badRequestWrongContentTypeJsonResponse))
          }
        }
      }
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          assertRight(expectedEntity = "This is a valid plaintext response") {
            client.getPlainText(accessToken = None)(validPlainTextResponseGetEndpoint[String])
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(expectedEntity = MyEntity(Some("This is a valid plaintext response"))) {
            client.getPlainText(accessToken = None)(validPlainTextResponseGetEndpoint[MyEntity])
          }
        }

        "respond with empty string if the response body is empty" in {
          assertRight(expectedEntity = "") {
            client.getPlainText(accessToken = None)(getEndpoint[String](okEmptyPlainTextResponse))
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            client.getJson(accessToken = None)(getEndpoint[String](notFoundPlainTextResponse))
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(None)) {
            client.getPlainText(accessToken = None)(getEndpoint[MyEntity](notFoundPlainTextResponse))
          }
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" in {
          assert500 {
            client.getJson[Json](accessToken = None)(timeoutResponseGetEndpoint)
          }
        }

        "return error with response status and empty message" in {
          assertLeft(Status.NotFound, expectedErrorMessage = "") {
            client.getJson(accessToken = None)(getEndpoint[String](notFoundEmptyPlainTextBodyResponse))
          }
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType, expectedErrorMessage = "`Content-Type` not provided") {
            client.getPlainText(accessToken = None)(getEndpoint[String](badRequestNoContentTypeJsonResponse))
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType,
                     expectedErrorMessage = "multipart/form-data: unexpected `Content-Type`") {
            client.getPlainText(accessToken = None)(getEndpoint[String](badRequestWrongContentTypeJsonResponse))
          }
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with entity body and json response" when {

      "response is 200" should {

        def validResponsePostEndpoint[R]: FsClientRequestWithBody[MyRequestBody, R] =
          postEndpoint(okJsonResponse, requestBody)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            client.fetchJson[MyRequestBody, Json](accessToken = None)(validResponsePostEndpoint)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          case class ValidEntity(message: String)
          assertRight(ValidEntity("this is a json response")) {
            client.fetchJson(accessToken = None)(validResponsePostEndpoint[ValidEntity])
          }
        }

        "respond with error if the response json is unexpected" in {
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.fetchJson(accessToken = None)(validResponsePostEndpoint[InvalidEntity])
          }
        }

        "respond with error if the response body is empty" in {
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            client.fetchJson(accessToken = None)(validResponsePostEndpoint[InvalidEntity])
          }
        }
      }

      "response is 404" should {
        def notFoundJsonResponsePostEndpoint: FsClientRequestWithBody[MyRequestBody, Json] =
          postEndpoint(notFoundJsonResponse, requestBody)

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            client.fetchJson(accessToken = None)(notFoundJsonResponsePostEndpoint)
          }
        }
      }

      "response is empty" should {
        def notFoundEmptyJsonResponsePostEndpoint: FsClientRequestWithBody[MyRequestBody, Json] =
          postEndpoint(notFoundEmptyJsonBodyResponse, requestBody)

        "respond with error for http response timeout" in {
          assert500 {
            client.fetchJson[MyRequestBody, Json](accessToken = None)(timeoutResponsePostEndpoint(requestBody))
          }
        }

        "return error with response status and default message" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.emptyResponse) {
            client.fetchJson(accessToken = None)(notFoundEmptyJsonResponsePostEndpoint)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType, expectedErrorMessage = "`Content-Type` not provided") {
            client.fetchJson[MyRequestBody, Json](accessToken = None)(
              postEndpoint(badRequestNoContentTypeJsonResponse, requestBody)
            )
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType,
                     expectedErrorMessage = "multipart/form-data: unexpected `Content-Type`") {
            client.fetchJson(accessToken = None)(
              postEndpoint[MyRequestBody, Json](badRequestWrongContentTypeJsonResponse, requestBody)
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
              .fetchPlainText(accessToken = None) {
                validPlainTextResponsePostEndpoint[MyRequestBody, String](requestBody)
              }
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(Some("This is a valid plaintext response"))) {
            client
              .fetchPlainText[MyRequestBody, MyEntity](accessToken = None) {
                validPlainTextResponsePostEndpoint(requestBody)
              }
          }
        }

        "respond with empty string if the response body is empty" in {
          assertRight(expectedEntity = "") {
            client
              .fetchPlainText(accessToken = None) {
                postEndpoint[MyRequestBody, String](okEmptyPlainTextResponse, requestBody)
              }
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            client.fetchJson(accessToken = None) {
              postEndpoint[MyRequestBody, String](notFoundPlainTextResponse, requestBody)
            }
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(None)) {
            client
              .fetchPlainText(accessToken = None) {
                postEndpoint[MyRequestBody, MyEntity](notFoundPlainTextResponse, requestBody)
              }
          }
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" in {
          assert500 {
            client.fetchJson(accessToken = None)(timeoutResponsePostEndpoint[MyRequestBody, String](requestBody))
          }
        }

        "return error with response status and empty message" in {
          assertLeft(Status.NotFound, expectedErrorMessage = "") {
            client
              .fetchJson(accessToken = None)(
                postEndpoint[MyRequestBody, String](notFoundEmptyPlainTextBodyResponse, requestBody)
              )
          }
        }
      }

      "response has no `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType, expectedErrorMessage = "`Content-Type` not provided") {
            client
              .fetchPlainText(accessToken = None)(
                postEndpoint[MyRequestBody, String](badRequestNoContentTypeJsonResponse, requestBody)
              )
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return 415 with the right error" in {
          assertLeft(Status.UnsupportedMediaType,
                     expectedErrorMessage = "multipart/form-data: unexpected `Content-Type`") {
            client
              .fetchPlainText(accessToken = None)(
                postEndpoint[MyRequestBody, String](badRequestWrongContentTypeJsonResponse, requestBody)
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
                    Left(ResponseError(new Exception(s"Unexpected response:\n[$invalid]")))
                }
            )
          )

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
