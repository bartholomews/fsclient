package fsclient.config

import fsclient.entities.OAuthVersion.V1
import fsclient.entities.OAuthVersion.V1.{AccessToken, BasicSignature}
import fsclient.entities.{OAuthDisabled, OAuthEnabled}
import org.http4s.client.oauth1.{Consumer, Token}
import org.scalatest.{FunSuite, Inside, Matchers}
import pureconfig.error.{ConfigReaderException, ConfigReaderFailures}

class FsClientConfigTest extends FunSuite with Matchers with Inside {

  private val sampleUserAgent = UserAgent(
    appName = "mock-app",
    appVersion = None,
    appUrl = Some("url")
  )

  private val sampleAccessToken = AccessToken(
    Token("TOKEN_KEY", "TOKEN_SECRET"),
    Consumer("CONSUMER_KEY", "CONSUMER_SECRET")
  )

  test("disabled") {
    FsClientConfig.disabled(sampleUserAgent) shouldBe FsClientConfig(sampleUserAgent, OAuthDisabled)
  }

  test("v1.basic() with valid config") {
    FsClientConfig.v1.basic().orThrow shouldBe FsClientConfig(
      userAgent = UserAgent(appName = "mock-app-outer", appVersion = Some("2.2.1"), appUrl = None),
      authInfo = OAuthEnabled(
        BasicSignature(Consumer(key = "mock-consumer-key-outer", secret = "mock-consumer-secret-outer"))
      )
    )
  }

  test("v1.basic(key: String) with valid config") {
    FsClientConfig.v1.basic("mock-app").orThrow shouldBe FsClientConfig(
      userAgent = UserAgent(appName = "mock-app", appVersion = Some("0.0.1"), appUrl = None),
      authInfo = OAuthEnabled(
        BasicSignature(Consumer(key = "mock-consumer-key", secret = "mock-consumer-secret"))
      )
    )
  }

  test("v1.basic(key: String) with invalid config") {
    intercept[ConfigReaderException[FsClientConfig.Config]] {
      FsClientConfig.v1.basic("unknown-key").orThrow
    }
  }

  test("v1.token() with missing token config") {
    inside(FsClientConfig.v1.token()) {
      case Left(ConfigReaderFailures(failure, Nil)) => failure.description shouldBe "Key not found: 'access-token'."
    }
  }

  test("v1.token(consumerConfig: ConsumerConfig, token: Token)") {
    FsClientConfig.v1.token(sampleUserAgent, sampleAccessToken) shouldBe FsClientConfig(
      userAgent = sampleUserAgent,
      authInfo = OAuthEnabled(sampleAccessToken)
    )
  }

  test("v1.token(key: String) with valid config") {
    FsClientConfig.v1.token("mock-app") shouldBe Right(
      FsClientConfig(
        userAgent = UserAgent(appName = "mock-app", appVersion = Some("0.0.1"), appUrl = None),
        authInfo = OAuthEnabled(
          V1.AccessToken(Token("TOKENVALUE", "TOKENSECRET"), Consumer("mock-consumer-key", "mock-consumer-secret"))
        )
      )
    )
  }

  test("v1.token(key: String) with invalid config") {
    intercept[ConfigReaderException[FsClientConfig.Config]] {
      FsClientConfig.v1.token("unknown-key").orThrow
    }
  }
}
