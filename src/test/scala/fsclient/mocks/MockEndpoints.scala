package fsclient.mocks

import fsclient.entities
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
  final val badRequestNoContentTypeNorBodyJsonResponse =
    "no-content-type-json-response"
  final val badRequestMultipartJsonResponse =
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

  def postJsonEndpoint[B](endpoint: String, requestBody: B): entities.FsClientRequestWithBody.PostJson[B] =
    new FsClientRequestWithBody.PostJson[B] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
      override val body: B = requestBody
    }

  def getEndpoint(endpoint: String): FsClientPlainRequest.Get = new FsClientPlainRequest.Get {
    override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
  }
}
