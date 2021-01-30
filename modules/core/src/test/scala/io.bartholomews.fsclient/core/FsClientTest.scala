package io.bartholomews.fsclient.core

import io.bartholomews.fsclient.client.IdentityClient
import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.oauth.ClientCredentials
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.Consumer
import org.scalatest.Inside
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import pureconfig.error.ConfigReaderFailures

class FsClientTest extends AsyncFunSuite with IdentityClient with Matchers with Inside {

  test("v1.clientCredentials with valid config") {
    FsClient.v1.clientCredentials(consumerNamespace = "mock-app")(backend) shouldBe Right(
      FsClient(
        userAgent = UserAgent(appName = "mock-app", appVersion = Some("0.0.1"), appUrl = None),
        signer = ClientCredentials(Consumer(key = "mock-consumer-key", secret = "mock-consumer-secret")),
        backend = backend
      )
    )
  }

  test("v1.clientCredentials with invalid config") {
    inside(FsClient.v1.clientCredentials(consumerNamespace = "unknown-key")(backend)) {
      case Left(ConfigReaderFailures(error)) =>
        error.description shouldBe "Key not found: 'unknown-key'."
    }
  }

// TODO
//  test("sttp.Uri configReader") {
//    ConfigSource.default
//      .at(consumerNamespace)
//  }
}
