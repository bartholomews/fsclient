package fsclient.mocks

import fsclient.entities.{HttpEndpoint, HttpMethod}
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
  final val badRequestNoContentTypeJsonResponse = "no-content-type-json-response"
  final val badRequestWrongContentTypeJsonResponse = "wrong-content-type-json-response"
  final val badRequestNoContentTypePlainTextResponse = "no-content-type-plaintext-response"
  final val badRequestWrongContentTypePlainTextResponse = "wrong-content-type-plaintext-response"
  final val timeoutResponse = "timeout-response"

  def postEndpoint[A](endpoint: String): HttpEndpoint[A] = new HttpEndpoint[A] with HttpMethod.POST {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
  }

  def getEndpoint[A](endpoint: String): HttpEndpoint[A] = new HttpEndpoint[A] with HttpMethod.GET {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
  }
}