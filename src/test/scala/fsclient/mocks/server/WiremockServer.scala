package fsclient.mocks.server

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, get, stubFor}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WiremockServer extends BeforeAndAfterAll {

  this: Suite =>

  val server: WireMockServer = new WireMockServer(new WireMockConfiguration().extensions(ResourceJsonTransformer))

  override def beforeAll: Unit = {
    server.start()
    stubApi()
  }

  override def afterAll: Unit = server.stop()

  private def stubApi(): Unit = {
    stubFor(get(anyUrl()).willReturn(aResponse().withTransformers(ResourceJsonTransformer.getName)))
    ()
  }
}
