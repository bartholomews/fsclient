package fsclient.http.client

import fsclient.entities.HttpResponse
import fsclient.mocks._
import fsclient.mocks.server.WiremockServer
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.tagobjects.Slow
import org.scalatest.{AsyncWordSpec, Matchers}
import reporter.TagsTracker

class IOClientTest extends AsyncWordSpec with Matchers with TagsTracker with WiremockServer {

  "A valid simple client" when {

    val client = validSimpleClient

    "calling a GET json endpoint" should {

      def res: HttpResponse[Json] = client.fetchJson(jsonResponseEndpoint).unsafeRunSync()

      "retrieve the json with Status Ok and entity" taggedAs Slow in {
        res.status shouldBe Status.Ok
        res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
      }
    }
  }
}
