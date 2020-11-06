package io.bartholomews.fsclient.mocks

import io.bartholomews.fsclient.codecs.ResDecoder
import io.bartholomews.fsclient.requests._
import io.circe.{Decoder, Encoder}
import org.http4s.Uri

trait MockEndpoints {

  val wiremockBaseUri = "http://127.0.0.1:8080"

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

  def postPlainTextEndpoint[B, R](endpoint: String, b: B)(implicit
    encode: Encoder[B],
    decoder: ResDecoder[String, R]
  ): FsSimplePlainText.Post[B, R] =
    new FsSimplePlainText.Post[B, R] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
      override def requestBody: B = b
    }

  def postJsonEndpoint[B, R](endpoint: String, b: B)(implicit
    encode: Encoder[B],
    decoder: Decoder[R]
  ): FsSimpleJson.Post[B, R] =
    new FsSimpleJson.Post[B, R] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
      override def requestBody: B = b
    }

  def getPlainTextEndpoint[Res](
    endpoint: String
  )(implicit decoder: ResDecoder[String, Res]): FsSimplePlainText.Get[Res] =
    new FsSimplePlainText.Get[Res] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
    }

  def getJsonEndpoint[Res](endpoint: String)(implicit decoder: Decoder[Res]): FsSimpleJson.Get[Res] =
    new FsSimpleJson.Get[Res] {
      override val uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
    }
}
