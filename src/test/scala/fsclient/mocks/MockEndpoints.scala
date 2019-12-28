package fsclient.mocks

import fsclient.entities.OAuthVersion.OAuthV1.{AccessTokenRequestV1, RequestTokenV1}
import fsclient.requests._
import io.circe.Json
import org.http4s.Uri
import org.http4s.client.oauth1.Consumer

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

  def validAccessTokenEndpointV1(implicit consumer: Consumer): AccessTokenRequestV1 =
    AccessTokenRequestV1(
      Uri.unsafeFromString(s"$wiremockBaseUri/$okAccessTokenResponse"),
      RequestTokenV1(validToken, "")
    )

  def postPlainTextEndpoint[B, R](endpoint: String, requestBody: B): FsSimpleRequestWithBody.Post[B, String, R] =
    new FsSimpleRequestWithBody.Post[B, String, R] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
      override val body: B = requestBody
    }

  def postJsonEndpoint[B, R](endpoint: String, requestBody: B): FsSimpleRequestWithBody.Post[B, Json, R] =
    new FsSimpleRequestWithBody.Post[B, Json, R] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
      override val body: B = requestBody
    }

  def getPlainTextEndpoint[Res](endpoint: String): FsSimpleRequest.Get[String, Res] =
    new FsSimpleRequest.Get[String, Res] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
    }

  def getJsonEndpoint[Res](endpoint: String): FsSimpleRequest.Get[Json, Res] =
    new FsSimpleRequest.Get[Json, Res] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
    }
}
