package fsclient.http.client

import cats.effect.IO
import cats.implicits._
import fsclient.mocks.server.{OAuthServer, WiremockServer}
import fsclient.oauth.OAuthVersion.OAuthV1.AccessTokenV1
import fsclient.requests._
import fsclient.utils.HttpTypes.HttpPipe
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.Status
import org.http4s.client.oauth1.Consumer
import org.scalatest.WordSpec
import org.scalatest.tagobjects.Slow

class IOClientTest extends WordSpec with IOClientMatchers with WiremockServer with OAuthServer {

  "A valid simple client with no OAuth" when {

    val client = validSimpleClient()

    def validPlainTextResponseGetEndpoint[R]: FsSimplePlainTextRequest.Get[R] =
      getPlainTextEndpoint[R](okPlainTextResponse)

    def validPlainTextResponsePostEndpoint[B, R](body: B): FsSimplePlainTextRequestWithBody[B, R] =
      postPlainTextEndpoint(okPlainTextResponse, body)

    def timeoutResponseGetEndpoint[R]: FsSimpleJsonRequest.Get[R] =
      getJsonEndpoint(timeoutResponse)

    def timeoutResponsePostEndpoint[B, R](body: B): FsSimpleJsonRequestWithBody[B, R] =
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

        def validResponseGetEndpoint[R]: FsSimpleJsonRequest.Get[R] =
          getJsonEndpoint(okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            validResponseGetEndpoint[Json].runWith(client)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          assertRight(ValidEntity("this is a json response")) {
            validResponseGetEndpoint[ValidEntity].runWith(client)
          }
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            validResponseGetEndpoint[InvalidEntity].runWith(client)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            validResponseGetEndpoint[InvalidEntity].runWith(client)
          }
        }
      }

      "response is 404" should {
        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            getJsonEndpoint[Json](notFoundJsonResponse).runWith(client)
          }
        }
      }

      "response is empty" should {

        def notFoundEmptyJsonResponseGetEndpoint[Res]: FsSimpleJsonRequest.Get[Res] =
          getJsonEndpoint(notFoundEmptyJsonBodyResponse)

        "respond with error for http response timeout" taggedAs Slow in {
          assertEmptyResponseError {
            timeoutResponseGetEndpoint[Json].runWith(client)
          }
        }

        "return error with response status and default message" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.emptyResponse) {
            notFoundEmptyJsonResponseGetEndpoint[Json].runWith(client)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME: expectedErrorMessage
            getJsonEndpoint[Json](badRequestNoContentTypeNorBodyJsonResponse).runWith(client)
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "response=true&urlencoded=example") {
            getJsonEndpoint[Json](badRequestMultipartJsonResponse).runWith(client)
          }
        }
      }
    }
    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          assertRight(expectedEntity = "This is a valid plaintext response") {
            validPlainTextResponseGetEndpoint[String].runWith(client)
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(expectedEntity = MyEntity(Some("This is a valid plaintext response"))) {
            validPlainTextResponseGetEndpoint[MyEntity].runWith(client)
          }
        }

        "respond with empty string if the response body is empty" in {
          assertRight(expectedEntity = "") {
            getJsonEndpoint(okEmptyPlainTextResponse).runWith(client)
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            getJsonEndpoint[Json](notFoundPlainTextResponse).runWith(client)
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(None)) {
            getPlainTextEndpoint[MyEntity](notFoundPlainTextResponse).runWith(client)
          }
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" taggedAs Slow in {
          assertEmptyResponseError {
            timeoutResponseGetEndpoint[Json].runWith(client)
          }
        }

        "return error with response status and empty message" in {
          assertLeft(Status.NotFound, expectedErrorMessage = "") {
            getJsonEndpoint[Json](notFoundEmptyPlainTextBodyResponse).runWith(client)
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME: expectedErrorMessage
            getJsonEndpoint[String](badRequestNoContentTypeNorBodyJsonResponse).runWith(client)
          }
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with entity body and json response" when {

      "response is 200" should {

        def validResponsePostJsonEndpoint[R]: FsSimpleJsonRequestWithBody[MyRequestBody, R] =
          postJsonEndpoint[MyRequestBody, R](okJsonResponse, requestBody)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            validResponsePostJsonEndpoint[Json].runWith(client)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          assertRight(ValidEntity("this is a json response")) {
            validResponsePostJsonEndpoint[ValidEntity].runWith(client)
          }
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            validResponsePostJsonEndpoint[InvalidEntity].runWith(client)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          assertDecodingFailure {
            validResponsePostJsonEndpoint[InvalidEntity].runWith(client)
          }
        }
      }

      "response is 404" should {
        def notFoundJsonResponsePostEndpoint[R]: FsSimpleJsonRequestWithBody[MyRequestBody, R] =
          postJsonEndpoint(notFoundJsonResponse, requestBody)

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            notFoundJsonResponsePostEndpoint[Json].runWith(client)
          }
        }
      }

      "response is empty" should {
        def notFoundEmptyJsonResponsePostEndpoint[R]: FsSimpleJsonRequestWithBody[MyRequestBody, R] =
          postJsonEndpoint(notFoundEmptyJsonBodyResponse, requestBody)

        "respond with error for http response timeout" taggedAs Slow in {
          assertEmptyResponseError {
            timeoutResponsePostEndpoint[MyRequestBody, Json](requestBody).runWith(client)
          }
        }

        "return error with response status and default message" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.emptyResponse) {
            notFoundEmptyJsonResponsePostEndpoint[Json].runWith(client)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME: expectedErrorMessage
            postJsonEndpoint[MyRequestBody, Json](badRequestNoContentTypeNorBodyJsonResponse, requestBody)
              .runWith(client)
          }
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with plainText response" when {

      "response is 200" should {

        "retrieve the response with Status Ok and string entity" in {
          assertRight("This is a valid plaintext response") {
            validPlainTextResponsePostEndpoint[MyRequestBody, String](requestBody).runWith(client)
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(Some("This is a valid plaintext response"))) {
            validPlainTextResponsePostEndpoint[MyRequestBody, MyEntity](requestBody).runWith(client)
          }
        }

        "respond with empty string if the response body is empty" in {
          assertRight(expectedEntity = "") {
            postPlainTextEndpoint[MyRequestBody, String](okEmptyPlainTextResponse, requestBody).runWith(client)
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            postJsonEndpoint[MyRequestBody, Json](notFoundPlainTextResponse, requestBody).runWith(client)
          }
        }

        "respond applying the provided `plainTextDecoder`" in {
          case class MyEntity(str: Option[String])

          implicit val plainTextDecoder: HttpPipe[IO, String, MyEntity] =
            _.map(e => MyEntity(e.toOption).asRight[ResponseError])

          assertRight(MyEntity(None)) {
            postPlainTextEndpoint[MyRequestBody, MyEntity](notFoundPlainTextResponse, requestBody).runWith(client)
          }
        }
      }

      "response is empty" should {

        "respond with error for http response timeout" taggedAs Slow in {
          assertEmptyResponseError {
            timeoutResponsePostEndpoint[MyRequestBody, Json](requestBody).runWith(client)
          }
        }

        "return error with response status and empty message" in {
          assertLeft(Status.NotFound, expectedErrorMessage = "") {
            postJsonEndpoint[MyRequestBody, Json](notFoundEmptyPlainTextBodyResponse, requestBody).runWith(client)
          }
        }
      }

      "response has no `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME expectedErrorMessage
            postJsonEndpoint[MyRequestBody, String](badRequestNoContentTypeNorBodyJsonResponse, requestBody)
              .runWith(client)
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "response=true&urlencoded=example") {
            postJsonEndpoint[MyRequestBody, String](badRequestMultipartJsonResponse, requestBody).runWith(client)
          }
        }
      }

      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

      "calling `accessTokenRequestV1`" should {
        "work" in {

          import org.http4s.client.oauth1.Token

          implicit val consumer: Consumer = validConsumer

          implicit val decoder: HttpPipe[IO, String, AccessTokenV1] = _.map(
            _.fold(
              err => Left(err),
              str =>
                str match {
                  case accessTokenResponseRegex(tokenValue, tokenSecret) =>
                    Right(AccessTokenV1(Token(tokenValue, tokenSecret)))
                  case invalid =>
                    Left(ResponseError(new Exception(s"Unexpected response:\n[$invalid]"), Status.UnsupportedMediaType))
                }
            )
          )

          val res =
            validSimpleClient().toOAuthClientV1(validAccessTokenEndpointV1).unsafeRunSync()

          inside(res) {
            case Right(oAuthClient) =>
              oAuthClient.consumer shouldBe client.consumer
          }
        }
      }
    }
  }
}

// TODO: test V2
// TODO: test `oAuthClient` calls with valid and invalid token
// TODO: test `simpleClient.auth` calls with valid and invalid token
