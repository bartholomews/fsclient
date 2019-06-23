package fsclient.mocks

import fsclient.entities.{HttpEndpoint, HttpMethod, GET}
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

  def notFoundJsonResponseEndpoint[M <: HttpMethod](httpMethod: M): HttpEndpoint[Json, M] =
    makeEndpoint(notFoundJsonResponse, httpMethod)

  def notFoundEmptyJsonResponseEndpoint[M <: HttpMethod](httpMethod: M): HttpEndpoint[Json, M] =
    makeEndpoint(notFoundEmptyJsonBodyResponse, httpMethod)

  /*
    valid endpoints NEED to match the string under `__files` dir
   */
  def validResponseEndpoint[A, M <: HttpMethod](httpMethod: M): HttpEndpoint[A, M] =
    makeEndpoint("test-json-response", httpMethod)

  def validPlainTextResponseEndpoint[A, M <: HttpMethod](httpMethod: M): HttpEndpoint[A, M] =
    makeEndpoint("test-plaintext-response", httpMethod)

  def timeoutResponseEndpoint[A, M <: HttpMethod](httpMethod: M): HttpEndpoint[A, M] =
    makeEndpoint(timeoutResponse, httpMethod)

  def makeEndpoint[A, M <: HttpMethod](endpoint: String, httpMethod: M): HttpEndpoint[A, M] = new HttpEndpoint[A, M] {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
    override val method: M = httpMethod
  }

  def getEndpoint[A](endpoint: String): HttpEndpoint[A, GET] = makeEndpoint(endpoint, GET())
}