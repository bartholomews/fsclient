package fsclient.http.client

import cats.implicits._
import fsclient.mocks._
import fsclient.mocks.server.WiremockServer
import io.circe.Json
import io.circe.syntax._
import org.http4s.Status
import org.scalatest.{Matchers, WordSpec}

// TODO IOWordSpec
class IOClientTest extends WordSpec with Matchers with WiremockServer {

  "A valid simple client" when {

    val client = validSimpleClient

    "calling a GET endpoint with Json response" when {

      "response is 200" should {
        def ioRes: client.IOResponse[Json] = client.fetchJson(validResponseEndpoint[Json])

        "retrieve the json with Status Ok and entity" in {
          val res = ioRes.unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
        }
      }

      "response is 404" should {
        def ioRes: client.IOResponse[Json] = client.fetchJson(notFoundJsonResponseEndpoint)

        "retrieve the json with Status NotFound" in {
          val res = ioRes.unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(e => e.getMessage) shouldBe Left(
            Map("message" -> "The requested resource was not found.").asJson.spaces2
          )
        }
      }

      //      "response is empty" should {
      //        def res: HttpResponse[Json] = client.fetchJson(emptyResponseEndpoint[Json]).unsafeRunSync()
      //
      //        "res error" in {
      //          res.status shouldBe Status.Ok
      //          res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
      //        }
      //      }
    }

    "calling a POST endpoint with no entity body and json response" in {
      pending
    }

    "calling a POST endpoint with entity body and json response" in {
      pending
    }

    "calling a GET endpoint with plainText response" in {
      pending
    }

    "calling a POST endpoint with plainText response" in {
      pending
    }

  }
}
