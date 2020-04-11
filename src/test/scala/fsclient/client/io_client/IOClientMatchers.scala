package fsclient.client.io_client

import fsclient.entities.{FsResponseErrorJson, FsResponseErrorString, FsResponseSuccess}
import fsclient.utils.HttpTypes.{HttpResponse, IOResponse}
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}

trait IOClientMatchers extends Matchers with Inside {

  def assertResponse[R](ioResponse: IOResponse[R])(pf: PartialFunction[HttpResponse[R], Assertion]): Assertion =
    inside(ioResponse.unsafeRunSync())(pf)

  def assertRight[R](expectedEntity: R)(ioResponse: IOResponse[R]): Assertion =
    assertResponse(ioResponse) {
      case FsResponseSuccess(_, status, entity) =>
        status shouldBe Status.Ok
        entity shouldBe expectedEntity
    }

  def assertErrorString[R](expectedStatus: Status, expectedError: String)(ioResponse: IOResponse[R]): Assertion =
    assertResponse(ioResponse) {
      case FsResponseErrorString(_, status, error) =>
        status shouldBe expectedStatus
        error shouldBe expectedError
    }

  def assertErrorJson[R](expectedStatus: Status, expectedError: Json)(ioResponse: IOResponse[R]): Assertion =
    assertResponse(ioResponse) {
      case FsResponseErrorJson(_, status, error) =>
        status shouldBe expectedStatus
        error shouldBe expectedError
    }

  def assertEmptyResponseError[R](ioResponse: IOResponse[R]): Assertion =
    assertErrorString(Status.NoContent, ExpectedErrorMessage.emptyResponse)(ioResponse)

  def assertDecodingFailure[R](ioResponse: IOResponse[R]): Assertion =
    assertErrorString(Status.UnprocessableEntity, ExpectedErrorMessage.decodingError)(ioResponse)
}

object ExpectedErrorMessage {
  val decodingError: String = "There was a problem decoding or parsing this response, please check the error logs"
  val notFoundString: String = "The requested resource was not found."
  val notFoundJson: Json = Map("message" -> notFoundString).asJson
  val emptyResponse: String = "Response body was empty"
}
