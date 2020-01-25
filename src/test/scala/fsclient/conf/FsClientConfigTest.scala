package fsclient.conf

import fsclient.entities.AuthEnabled
import fsclient.entities.AuthVersion.V1.BasicSignature
import org.http4s.client.oauth1.Consumer
import org.scalatest.{FunSuite, Matchers}
import pureconfig.error.ConfigReaderException

class FsClientConfigTest extends FunSuite with Matchers {

  test("v1(key: String) with valid config") {
    FsClientConfig.v1("mock-app") shouldBe FsClientConfig(
      userAgent = UserAgent(
        appName = "mock-app",
        appVersion = Some("0.0.1"),
        appUrl = None
      ),
      authInfo = AuthEnabled(
        BasicSignature(Consumer(key = "mock-consumer-key", secret = "mock-consumer-secret"))
      )
    )
  }

  test("v1(key: String) with invalid config") {
    intercept[ConfigReaderException[FsClientConfig.Config]] {
      FsClientConfig.v1("unknown-key")
    }
  }
}
