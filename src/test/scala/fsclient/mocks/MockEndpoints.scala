package fsclient.mocks

import fsclient.entities.HttpEndpoint
import io.circe.Json
import org.http4s.{Method, Uri}

trait MockEndpoints {

  private val wiremockBaseUri = "http://127.0.0.1:8080"

  private[mocks] final val notFoundJsonResponse = "not-found-json-response"
  private[mocks] final val notFoundPlainTextResponse = "not-found-plaintext-response"
  private[mocks] final val notFoundEmptyResponse = "not-found-empty-response"
  private[mocks] final val notFoundEmptyJsonBodyResponse = "empty-json-body-response"
  private[mocks] final val okEmptyResponse = "empty-response"

  val notFoundJsonResponseEndpoint: HttpEndpoint[Json] = getEndpoint[Json](notFoundJsonResponse)
  val notFoundPlainTextResponseEndpoint: HttpEndpoint[String] = getEndpoint[String](notFoundPlainTextResponse) // TODO
  val notFoundEmptyResponseEndpoint: HttpEndpoint[Nothing] = getEndpoint[Nothing](notFoundEmptyResponse) // TODO
  val emptyResponseEndpoint: HttpEndpoint[Nothing] = getEndpoint[Nothing](okEmptyResponse) // TODO
  /*
    valid endpoints NEED to match the string under `__files` dir
   */
  def validResponseEndpoint[T]: HttpEndpoint[T] = getEndpoint[T]("test-json-response")


  def getEndpoint[A](endpoint: String): HttpEndpoint[A] = new HttpEndpoint[A] {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")

    override def method: Method = Method.GET
  }

}
