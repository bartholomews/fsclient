package io.bartholomews.fsclient.config

import io.bartholomews.fsclient.entities.oauth.{AuthDisabled, ClientCredentials}
import org.http4s.client.oauth1.Consumer
import org.scalatest.Inside
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.error.ConfigReaderException

class FsClientConfigTest extends AsyncFunSuite with Matchers with Inside {

  private val sampleUserAgent = UserAgent(
    appName = "mock-app",
    appVersion = None,
    appUrl = Some("url")
  )

  test("disabled") {
    FsClientConfig.disabled(sampleUserAgent) shouldBe FsClientConfig(sampleUserAgent, AuthDisabled)
  }

  test("v1.basic() with valid config") {
    FsClientConfig.v1.basic(consumerNamespace = "mock-app").orThrow shouldBe FsClientConfig(
      userAgent = UserAgent(appName = "mock-app", appVersion = Some("0.0.1"), appUrl = None),
      signer = ClientCredentials(Consumer(key = "mock-consumer-key", secret = "mock-consumer-secret"))
    )
  }

  test("v1.basic(key: String) with invalid config") {
    a[ConfigReaderException[Consumer]] shouldBe thrownBy {
      FsClientConfig.v1.basic(consumerNamespace = "unknown-key").orThrow
    }
  }
}
