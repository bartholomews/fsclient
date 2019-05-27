package fsclient.mocks

import fsclient.entities.{HttpEndpoint, HttpMethod}
import io.circe.Json
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
  final val timeoutResponse = "timeout-response"

  val notFoundJsonResponseEndpoint: HttpEndpoint[Json] = getEndpoint(notFoundJsonResponse)
  val notFoundEmptyJsonResponseEndpoint: HttpEndpoint[Json] = getEndpoint(notFoundEmptyJsonBodyResponse)
  /*
    valid endpoints NEED to match the string under `__files` dir
   */
  def validResponseEndpoint[A]: HttpEndpoint[A] = getEndpoint[A]("test-json-response")
  def validPlainTextResponseEndpoint[A]: HttpEndpoint[A] = getEndpoint("test-plaintext-response")

  def timeoutResponseEndpoint[A]: HttpEndpoint[A] = getEndpoint[A](timeoutResponse)

  def getEndpoint[A](endpoint: String): HttpEndpoint[A] = new HttpEndpoint[A] with HttpMethod.GET {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
  }

}
