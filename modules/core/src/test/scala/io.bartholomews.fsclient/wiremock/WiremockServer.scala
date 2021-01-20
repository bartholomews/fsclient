package io.bartholomews.fsclient.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WiremockServer extends BeforeAndAfterAll {

  this: Suite =>

  val wiremockBaseUri = "http://127.0.0.1:8080"

  private val server: WireMockServer = new WireMockServer(
    new WireMockConfiguration()
      .withRootDirectory("modules/core/src/test/resources")
  )

  override def beforeAll(): Unit =
    server.start()

  override def afterAll(): Unit = server.stop()
}
