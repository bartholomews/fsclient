package fsclient.client.io_client

import cats.effect.IO
import fs2.Pipe
import fsclient.codecs.FsJsonResponsePipe
import fsclient.entities.{EmptyResponseException, HttpResponse, ResponseError}
import fsclient.implicits._
import fsclient.mocks.server.{OAuthServer, WiremockServer}
import fsclient.requests._
import io.circe.generic.extras.Configuration
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import org.http4s.Status
import org.scalatest.WordSpec
import org.scalatest.tagobjects.Slow

class IOClientTest extends WordSpec with IOClientMatchers with WiremockServer with OAuthServer {

  "A valid simple client with no OAuth" when {

    val client: IOClient = validSimpleClient()

    def validPlainTextResponseGetEndpoint[R]: FsSimpleRequest.Get[Nothing, String, R] =
      getPlainTextEndpoint[R](okPlainTextResponse)

    def validPlainTextResponsePostEndpoint[B, R](body: B): FsSimpleRequest[B, String, R] =
      postPlainTextEndpoint(okPlainTextResponse, body)

    def timeoutResponseGetEndpoint[R]: FsSimpleRequest.Get[Nothing, Json, R] =
      getJsonEndpoint(timeoutResponse)

    def timeoutResponsePostEndpoint[B, R](body: B): FsSimpleRequest[B, Json, R] =
      postJsonEndpoint(timeoutResponse, body)

    case class MyRequestBody(a: String, b: List[Int])
    object MyRequestBody extends FsJsonResponsePipe[MyRequestBody] {
      import io.circe.generic.semiauto._
      implicit val encoder: Encoder[MyRequestBody] = deriveEncoder
      implicit val decoder: Decoder[MyRequestBody] = deriveDecoder
    }

    val requestBody: MyRequestBody = MyRequestBody("A", List(1, 2, 3))

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a GET endpoint with Json response" when {

      "response is 200" should {

        def validResponseGetEndpoint[R]: FsSimpleRequest.Get[Nothing, Json, R] =
          getJsonEndpoint(okJsonResponse)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            validResponseGetEndpoint[Json].runWith(client)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          case class ValidEntity(message: String)
          object ValidEntity extends FsJsonResponsePipe[ValidEntity] {
            implicit val decoder: Decoder[ValidEntity] = io.circe.generic.semiauto.deriveDecoder
          }
          assertRight(ValidEntity("this is a json response")) {
            val r: FsSimpleRequest.Get[Nothing, Json, ValidEntity] = validResponseGetEndpoint[ValidEntity]
            r.runWith[IO](client)
          }
        }

        "respond with error if the response json is unexpected" in {
          case class InvalidEntity(something: Boolean)
          object InvalidEntity extends FsJsonResponsePipe[InvalidEntity] {
            implicit val conf: Configuration = io.circe.generic.extras.defaults.defaultGenericConfiguration
            implicit val dec: Decoder[InvalidEntity] = io.circe.generic.extras.semiauto.deriveConfiguredDecoder
          }
          assertDecodingFailure {
            validResponseGetEndpoint[InvalidEntity].runWith(client)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          implicit val entityPipe: Pipe[IO, Json, InvalidEntity] =
            fsclient.implicits.deriveJsonPipe[IO, InvalidEntity]
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

        def notFoundEmptyJsonResponseGetEndpoint[Res]: FsSimpleRequest.Get[Nothing, Json, Res] =
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
          case class MyEntity(str: String)

          implicit val plainTextDecoder: Pipe[IO, String, MyEntity] =
            _.map(str => MyEntity(str))

          assertRight(expectedEntity = MyEntity("This is a valid plaintext response")) {
            validPlainTextResponseGetEndpoint[MyEntity].runWith(client)
          }
        }

        "respond with `EmptyResponseException` if the response body is empty" in {
          assertResponse(getJsonEndpoint[String](okEmptyPlainTextResponse).runWith(client)) {
            case response @ HttpResponse(_, Left(error: ResponseError)) =>
              response.status shouldBe Status.UnprocessableEntity
              error shouldBe ResponseError(EmptyResponseException, Status.UnprocessableEntity)
          }
        }
      }

      "response is 404" should {

        "retrieve the string response with Status NotFound" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundString) {
            getJsonEndpoint[Json](notFoundPlainTextResponse).runWith(client)
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
          import fsclient.implicits._
          assertLeft(Status.BadRequest, expectedErrorMessage = "") {
            getJsonEndpoint[String](badRequestNoContentTypeNorBodyJsonResponse).runWith(client)
          }
        }
      }
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    "calling a POST endpoint with entity body and json response" when {

      "response is 200" should {

        def validResponsePostJsonEndpoint[R]: FsSimpleRequest[MyRequestBody, Json, R] =
          postJsonEndpoint[MyRequestBody, R](okJsonResponse, requestBody)

        "retrieve the json with Status Ok and entity" in {
          assertRight(Map("message" -> "this is a json response").asJson) {
            validResponsePostJsonEndpoint[Json].runWith(client)
          }
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          implicit val entityPipe: Pipe[IO, Json, ValidEntity] =
            fsclient.implicits.deriveJsonPipe[IO, ValidEntity]
          assertRight(ValidEntity("this is a json response")) {
            validResponsePostJsonEndpoint[ValidEntity].runWith(client)
          }
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          implicit val entityPipe: Pipe[IO, Json, InvalidEntity] =
            fsclient.implicits.deriveJsonPipe[IO, InvalidEntity]
          assertDecodingFailure {
            validResponsePostJsonEndpoint[InvalidEntity].runWith(client)
          }
        }

        "respond with error if the response body is empty" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          implicit val entityPipe: Pipe[IO, Json, InvalidEntity] =
            fsclient.implicits.deriveJsonPipe[IO, InvalidEntity]
          assertDecodingFailure {
            validResponsePostJsonEndpoint[InvalidEntity].runWith(client)
          }
        }
      }

      "response is 404" should {
        def notFoundJsonResponsePostEndpoint[R]: FsSimpleRequest[MyRequestBody, Json, R] =
          postJsonEndpoint(notFoundJsonResponse, requestBody)

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          assertLeft(Status.NotFound, ExpectedErrorMessage.notFoundJson) {
            notFoundJsonResponsePostEndpoint[Json].runWith(client)
          }
        }
      }

      "response is empty" should {
        def notFoundEmptyJsonResponsePostEndpoint[R]: FsSimpleRequest[MyRequestBody, Json, R] =
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

          implicit val plainTextDecoder: Pipe[IO, String, MyEntity] =
            _.map(str => MyEntity(Some(str)))

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
          import fsclient.implicits._
          assertLeft(Status.BadRequest, expectedErrorMessage = "") { // FIXME expectedErrorMessage
            postJsonEndpoint[MyRequestBody, String](badRequestNoContentTypeNorBodyJsonResponse, requestBody)
              .runWith(client)
          }
        }
      }

      "response has an unexpected `Content-Type`" should {
        "return the error status with the right message" in {
          assertLeft(Status.BadRequest, expectedErrorMessage = "response=true&urlencoded=example") {
            import fsclient.implicits._
            postJsonEndpoint[MyRequestBody, String](badRequestMultipartJsonResponse, requestBody).runWith(client)
          }
        }
      }

      // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    }
  }
}

// TODO: test V2
// TODO: test `oAuthClient` calls with valid and invalid token
// TODO: test `simpleClient.auth` calls with valid and invalid token
