package io.bartholomews.scalatestudo.matchers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{Assertion, Inside}
import sttp.client3.{Response, ResponseException}

trait StubbedIO extends Inside {

  /**
   *  Assert with a partial function on sttp `Response` given a request and a `StubMapping` stub
   *
   *  "the server responds with the expected string message" should {
   *
   *    def request: IOResponse[String] = sampleClient.someEndpoint.getSomeString
   *    def stub: StubMapping =
   *      stubFor(
   *        get(urlMatching("/some_endpoint"))
   *          .willReturn(
   *            aResponse()
   *              .withStatus(200)
   *              .withBody("Response text")
   *          )
   *       )
   *
   *  "return a Right with expected response" in matchResponse(stub, request) {
   *      case Response(Right(response), statusCode, _, _ , _) =>
   *          response shouldBe("Response text")
   *   }
   *
   * @param stubMapping the stub to be setup before making the request
   * @param request the request to make
   * @param pf a partial function with assertion based on the sttp `Response`
   * @tparam T the expected success response type
   * @return
   */
  def matchIdResponse[DE, T](
    stubMapping: => StubMapping,
    request: => Response[Either[ResponseException[String, DE], T]]
  )(
    pf: PartialFunction[Response[Either[ResponseException[String, DE], T]], Assertion]
  ): Assertion = {
    stubMapping
    inside(request)(pf)
  }

  def matchResponseBody[DE, T](
    stubMapping: => StubMapping,
    request: => Response[Either[ResponseException[String, DE], T]]
  )(
    pf: PartialFunction[Either[ResponseException[String, DE], T], Assertion]
  ): Assertion = {
    stubMapping
    inside(request.body)(pf)
  }
}
