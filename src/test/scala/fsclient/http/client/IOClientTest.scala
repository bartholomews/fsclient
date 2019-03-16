package fsclient.http.client

import fsclient.entities.HttpResponse
import fsclient.mocks._
import fsclient.mocks.server.WiremockServer
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.{Matchers, WordSpec}

class IOClientTest extends WordSpec with Matchers with WiremockServer {

  "A valid simple client" when {

    val client = validSimpleClient

    "calling a GET json endpoint" should {

      def res: HttpResponse[Json] = client.fetchJson(jsonResponseEndpoint).unsafeRunSync()

      "retrieve the json with Status Ok and entity" in {
        res.status shouldBe Status.Ok
        res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
      }
    }

  }
}
