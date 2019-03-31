package fsclient.mocks

import fsclient.entities.{HttpEndpoint, HttpMethod}
import io.circe.Json
import org.http4s.Uri

trait MockEndpoints {

  private val wiremockBaseUri = "http://127.0.0.1:8080"

  private[mocks] final val notFoundJsonResponse = "not-found-json-response"
  private[mocks] final val notFoundPlainTextResponse = "not-found-plaintext-response"
  private[mocks] final val notFoundEmptyResponse = "not-found-empty-response"
  private[mocks] final val notFoundEmptyJsonBodyResponse = "empty-json-body-response"
  private[mocks] final val okEmptyResponse = "empty-response"

  val notFoundJsonResponseEndpoint: HttpEndpoint[Json] = getEndpoint[Json](notFoundJsonResponse)
  val notFoundEmptyJsonResponseEndpoint: HttpEndpoint[Json] = getEndpoint[Json](notFoundEmptyJsonBodyResponse)
  val notFoundPlainTextResponseEndpoint: HttpEndpoint[String] = getEndpoint[String](notFoundPlainTextResponse) // TODO
  def notFoundEmptyResponseEndpoint[A]: HttpEndpoint[A] = getEndpoint[A](notFoundEmptyResponse) // TODO
  def emptyResponseEndpoint[A]: HttpEndpoint[A] = getEndpoint[A](okEmptyResponse) // TODO
  /*
    valid endpoints NEED to match the string under `__files` dir
   */
  def validResponseEndpoint[A]: HttpEndpoint[A] = getEndpoint[A]("test-json-response")

  def getEndpoint[A](endpoint: String): HttpEndpoint[A] = new HttpEndpoint[A] with HttpMethod.GET {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")
  }

}
