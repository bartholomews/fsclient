package fsclient.mocks.server

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import fsclient.mocks.MockClientConfig
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WiremockServer extends BeforeAndAfterAll with MockClientConfig {

  this: Suite =>

  val server: WireMockServer = new WireMockServer(new WireMockConfiguration().extensions(ResourceJsonTransformer))

  override def beforeAll: Unit = {
    server.start()
    stubApi()
  }

  override def afterAll: Unit = server.stop()

  private def stubApi(): Unit = {

    stubFor(get(anyUrl()).willReturn(aResponse().withTransformers(ResourceJsonTransformer.getName)))

    stubFor(post("/oauth/access_token")
      .willReturn(aResponse()
        .withBody(s"oauth_token=$validOAuthTokenValue" +
          s"&oauth_token_secret=$validOAuthTokenSecret"
        )
        .withTransformers(
          AuthenticatedRequestTransformer.getName,
          ValidateTokenRequestBodyTransformer.getName
        ))
    )

    ()
  }
}
