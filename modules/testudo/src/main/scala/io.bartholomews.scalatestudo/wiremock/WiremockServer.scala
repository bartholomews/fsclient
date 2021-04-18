package io.bartholomews.scalatestudo.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, anyUrl, get, stubFor}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEachTestData, Suite, TestData}

trait WiremockServer extends BeforeAndAfterAll with BeforeAndAfterEachTestData with WiremockUtils {
  self: Suite =>

  def testResourcesFileRoot: String // "src/test/resources"

  private lazy val server: WireMockServer = new WireMockServer(
    new WireMockConfiguration()
      .withRootDirectory(testResourcesFileRoot)
      .extensions(ResourceFileJsonTransformer)
  )

  override def beforeAll(): Unit =
    server.start()

  override def afterAll(): Unit =
    server.stop()

  def stubWithResourceFile: StubMapping =
    stubFor(
      get(anyUrl())
        .willReturn(
          aResponse()
            .withStatus(200)
            .withTransformers(ResourceFileJsonTransformer.getName)
        )
    )

  override protected def afterEach(testData: TestData): Unit = {
    server.resetAll()
    super.afterEach(testData)
  }
}
