package fsclient.mocks

import fsclient.entities.HttpEndpoint
import io.circe.Json
import org.http4s.{Method, Uri}

trait MockEndpoints {

  private val wiremockBaseUri = "http://127.0.0.1:8080"

  /*
    Endpoints NEED to match the string under `__files` dir
   */
  val jsonResponseEndpoint: HttpEndpoint[Json] = validGetEndpoint[Json]("test-json-response")
  val notFoundResponseEndpoint: String = "not-found"
  val emptyResponseEndpoint: String = "empty-response"

  def validGetEndpoint[A](endpoint: String): HttpEndpoint[A] = new HttpEndpoint[A] {
    override def uri: Uri = Uri.unsafeFromString(s"$wiremockBaseUri/$endpoint")

    override def method: Method = Method.GET
  }

}
