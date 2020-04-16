package io.bartholomews.fsclient.client

import io.bartholomews.fsclient.entities.{FsResponseErrorJson, FsResponseErrorString, FsResponseSuccess, OAuthVersion}
import io.bartholomews.fsclient.utils.HttpTypes.{HttpResponse, IOResponse}
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, Inside}

trait IOClientMatchers extends Matchers with Inside {

  def assertResponse[V <: OAuthVersion, R](
    ioResponse: IOResponse[V, R]
  )(pf: PartialFunction[HttpResponse[V, R], Assertion]): Assertion =
    inside(ioResponse.unsafeRunSync())(pf)

  def assertRight[V <: OAuthVersion, R](expectedEntity: R)(ioResponse: IOResponse[V, R]): Assertion =
    assertResponse(ioResponse) {
      case FsResponseSuccess(_, _, status, entity) =>
        status shouldBe Status.Ok
        entity shouldBe expectedEntity
    }

  def assertErrorString[V <: OAuthVersion, R](expectedStatus: Status, expectedError: String)(
    ioResponse: IOResponse[V, R]
  ): Assertion =
    assertResponse(ioResponse) {
      case FsResponseErrorString(_, _, status, error) =>
        status shouldBe expectedStatus
        error shouldBe expectedError
    }

  def assertErrorJson[V <: OAuthVersion, R](expectedStatus: Status, expectedError: Json)(
    ioResponse: IOResponse[V, R]
  ): Assertion =
    assertResponse(ioResponse) {
      case FsResponseErrorJson(_, _, status, error) =>
        status shouldBe expectedStatus
        error shouldBe expectedError
    }

  def assertEmptyResponseError[V <: OAuthVersion, R](ioResponse: IOResponse[V, R]): Assertion =
    assertErrorString(Status.NoContent, ExpectedErrorMessage.emptyResponse)(ioResponse)

  def assertDecodingFailure[V <: OAuthVersion, R](ioResponse: IOResponse[V, R]): Assertion =
    assertErrorString(Status.UnprocessableEntity, ExpectedErrorMessage.decodingError)(ioResponse)
}

object ExpectedErrorMessage {
  val decodingError: String = "There was a problem decoding or parsing this response, please check the error logs"
  val notFoundString: String = "The requested resource was not found."
  val notFoundJson: Json = Map("message" -> notFoundString).asJson
  val emptyResponse: String = "Response body was empty"
}
