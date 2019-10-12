package fsclient.mocks

import fsclient.entities._
import org.http4s.Uri

trait MockEndpoints {

  private val wiremockBaseUri = "http://127.0.0.1:8080"

  final val notFoundJsonResponse = "not-found-json-response"
  final val notFoundPlainTextResponse = "not-found-plaintext-response"
  final val notFoundEmptyResponse = "not-found-empty-response" // TODO
  final val notFoundEmptyJsonBodyResponse = "empty-json-body-response"
  final val notFoundEmptyPlainTextBodyResponse = "empty-plaintext-body-response"
  final val okEmptyResponse = "empty-response" // TODO
  final val okEmptyPlainTextResponse = "empty-plaintext-response"
  final val okJsonResponse = "test-json-response"
  final val okPlainTextResponse = "test-plaintext-response"
  final val badRequestNoContentTypeJsonResponse =
    "no-content-type-json-response"
  final val badRequestWrongContentTypeJsonResponse =
    "wrong-content-type-json-response"
  final val badRequestNoContentTypePlainTextResponse =
    "no-content-type-plaintext-response"
  final val badRequestWrongContentTypePlainTextResponse =
    "wrong-content-type-plaintext-response"
  final val timeoutResponse = "timeout-response"
  final val ok = "timeout-response"
  final val okAccessTokenResponse = "valid-access-token-plaintext-response"

  val validAccessTokenEndpoint =
    AccessTokenRequest(
      Uri.unsafeFromString(s"$wiremockBaseUri/$okAccessTokenResponse"),
      RequestToken(validToken, "")
    )

  def postEndpoint[B, R](endpoint: String, body: B): HttpRequest.POST[B, R] =
    HttpRequest.POST(
      Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint"),
      body
    )

  def getEndpoint[R](endpoint: String): HttpRequest.GET[R] = HttpRequest.GET(
    Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
  )
}
