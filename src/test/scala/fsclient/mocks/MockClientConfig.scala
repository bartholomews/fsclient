package fsclient.mocks

import fsclient.client.io_client.{IOAuthClient, IOClient}
import fsclient.config.{FsClientConfig, UserAgent}
import fsclient.entities.OAuthVersion.V1
import fsclient.entities.OAuthVersion.V1.BasicSignature
import fsclient.entities.{OAuthEnabled, OAuthVersion}
import org.http4s.client.oauth1.{Consumer, Token}
import org.scalatest.Assertions._

import scala.concurrent.ExecutionContext

trait MockClientConfig {

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  final val validConsumerKey = "VALID_CONSUMER_KEY"
  final val validConsumerSecret = "VALID_CONSUMER_SECRET"

  final val validConsumer = Consumer(validConsumerKey, validConsumerSecret)

  final val invalidConsumerKey = "INVALID_CONSUMER_KEY"
  final val invalidConsumerSecret = "INVALID_CONSUMER_SECRET"

  final val validOAuthTokenValue = "VALID_OAUTH_TOKEN_VALUE"
  final val validOAuthTokenSecret = "VALID_OAUTH_TOKEN_SECRET"
  final val validOAuthVerifier = "OAUTH_VERIFIER"

  final val validToken = Token(validOAuthTokenValue, validOAuthTokenSecret)

  // override val verifier: Option[String] = Some(validOAuthVerifier)

  def validSimpleClient(): IOClient[OAuthEnabled[V1.type]] =
    simpleClientWith(validConsumerKey, validConsumerSecret)

  def validOAuthClient[V <: OAuthVersion](authVersion: V): IOAuthClient[OAuthVersion.V1.type] =
    authVersion match {
      case OAuthVersion.V1 =>
        oAuthClientWith(validConsumerKey, validConsumerSecret, validToken)

      case OAuthVersion.V2 =>
        fail("OAuthV2 client: test setup not implemented")
    }

  def simpleClientWith(
    key: String,
    secret: String,
    appName: String = "someApp",
    appVersion: Option[String] = Some("1.0"),
    appUrl: Option[String] = Some("app.git")
  ): IOClient[OAuthEnabled[V1.type]] =
    new IOClient[OAuthEnabled[V1.type]] {
      UserAgent(appName, appVersion, appUrl)

      override def appConfig: FsClientConfig[OAuthEnabled[OAuthVersion.V1.type]] = FsClientConfig(
        UserAgent(appName, appVersion, appUrl),
        OAuthEnabled(BasicSignature(Consumer(key, secret)))
      )

      implicit override def ec: ExecutionContext = executionContext
    }

  def oAuthClientWith(
    key: String,
    secret: String,
    token: Token,
    appName: String = "someApp",
    appVersion: Option[String] = Some("1.0"),
    appUrl: Option[String] = Some("app.git")
  ): IOAuthClient[OAuthVersion.V1.type] = {
    val userAgent: UserAgent = UserAgent(appName, appVersion, appUrl)
    val consumer: Consumer = Consumer(key, secret)
    new IOAuthClient(userAgent, V1.AccessToken(token, consumer))
  }
}
