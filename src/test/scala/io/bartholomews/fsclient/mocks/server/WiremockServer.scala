package io.bartholomews.fsclient.mocks.server

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import io.bartholomews.fsclient.mocks.{MockClientConfig, MockEndpoints}
import org.scalatest.{BeforeAndAfterAll, Suite}

trait WiremockServer extends BeforeAndAfterAll with MockClientConfig with MockEndpoints {

  this: Suite =>

  val server: WireMockServer = new WireMockServer(
    new WireMockConfiguration().extensions(
      ResourceJsonTransformer,
      ResourcePlainTextTransformer
    )
  )

  override def beforeAll: Unit = {
    server.start()
    stubApi()
  }

  override def afterAll: Unit = server.stop()

  private def stubApi(): Unit = {

    stubFor(any(urlMatching("^.*json.*$")).willReturn(aResponse().withTransformers(ResourceJsonTransformer.getName)))
    stubFor(
      any(urlMatching("^.*plaintext.*$")).willReturn(aResponse().withTransformers(ResourcePlainTextTransformer.getName))
    )

    val timeout = 30000
    stubFor(any(urlMatching(s"/$timeoutResponse")).willReturn(aResponse().withFixedDelay(timeout)))

    stubFor(
      post("/oauth/access_token")
        .willReturn(
          aResponse()
            .withBody(
              s"oauth_token=$validOAuthTokenValue" +
                s"&oauth_token_secret=$validOAuthTokenSecret"
            )
            .withTransformers(
              AuthenticatedRequestTransformer.getName,
              ValidateTokenRequestBodyTransformer.getName
            )
        )
    )

    ()
  }
}
