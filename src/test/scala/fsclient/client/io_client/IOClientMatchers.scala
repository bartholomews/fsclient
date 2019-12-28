package fsclient.client.io_client

import fsclient.entities.HttpResponse
import fsclient.utils.HttpTypes.IOResponse
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.{Assertion, Inside, Matchers}

trait IOClientMatchers extends Matchers with Inside {

  def assertResponse[R](ioResponse: IOResponse[R])(pf: PartialFunction[HttpResponse[R], Assertion]): Assertion =
    inside(ioResponse.unsafeRunSync())(pf)

  def assertRight[R](expectedEntity: R)(ioResponse: IOResponse[R]): Assertion =
    assertResponse(ioResponse) {
      case response @ HttpResponse(_, Right(entity)) =>
        response.status shouldBe Status.Ok
        entity shouldBe expectedEntity
    }

  def assertLeft[R](expectedStatus: Status, expectedErrorMessage: String)(ioResponse: IOResponse[R]): Assertion =
    assertResponse(ioResponse) {
      case response @ HttpResponse(_, Left(error)) =>
        response.status shouldBe expectedStatus
        error.status shouldBe expectedStatus
        error.getMessage shouldBe expectedErrorMessage
    }

  def assertEmptyResponseError[R](ioResponse: IOResponse[R]): Assertion =
    assertLeft(Status.InternalServerError, ExpectedErrorMessage.emptyResponse)(ioResponse)

  def assertDecodingFailure[R](ioResponse: IOResponse[R]): Assertion =
    assertLeft(Status.InternalServerError, ExpectedErrorMessage.decodingError)(ioResponse)
}

object ExpectedErrorMessage {
  val decodingError: String = "There was a problem decoding or parsing this response, please check the error logs"
  val notFoundString: String = "The requested resource was not found."
  val notFoundJson: String = Map("message" -> notFoundString).asJson.spaces2
  val emptyResponse: String = "Response body was empty"
}
