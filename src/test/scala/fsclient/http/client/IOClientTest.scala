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

        "retrieve the json with Status Ok and entity" in {
          val res = client.fetchJson(validResponseEndpoint[Json]).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(Map("message" -> "this is a json response").asJson)
        }

        "retrieve the decoded json with Status Ok and entity" in {
          import io.circe.generic.auto._
          case class ValidEntity(message: String)
          val res = client.fetchJson(validResponseEndpoint[ValidEntity]).unsafeRunSync()
          res.status shouldBe Status.Ok
          res.entity shouldBe Right(ValidEntity("this is a json response"))
        }

        "respond with error if the response json is unexpected" in {
          import io.circe.generic.auto._
          case class InvalidEntity(something: Boolean)
          val res = client.fetchJson(validResponseEndpoint[InvalidEntity]).unsafeRunSync()
          res.status shouldBe Status.InternalServerError
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(err => {
            err.status shouldBe Status.InternalServerError
            err.getMessage shouldBe "There was a problem decoding or parsing this response, please check the error logs"
          })
        }
      }

      "response is 404" should {

        "retrieve the json response with Status NotFound and entity prettified with spaces2" in {
          val res = client.fetchJson(notFoundJsonResponseEndpoint).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity shouldBe a[Left[_, _]]
          res.entity.leftMap(e => e.getMessage) shouldBe Left(
            Map("message" -> "The requested resource was not found.").asJson.spaces2
          )
        }
      }

      "response is empty" should {

        "return error with response status and default message" in {
          val res = client.fetchJson(notFoundEmptyJsonResponseEndpoint).unsafeRunSync()
          res.status shouldBe Status.NotFound
          res.entity.leftMap(e => e.getMessage) shouldBe Left("Response was empty. Please check request logs")
        }
      }
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
