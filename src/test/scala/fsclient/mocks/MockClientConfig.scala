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

  def validSimpleClient(): IOClient[OAuthEnabled] =
    simpleClientWith(validConsumerKey, validConsumerSecret)

  def validOAuthClient(authVersion: OAuthVersion): IOAuthClient[authVersion.type] =
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
  ): IOClient[OAuthEnabled] =
    new IOClient[OAuthEnabled] {
      UserAgent(appName, appVersion, appUrl)

      override def appConfig: FsClientConfig[OAuthEnabled] = FsClientConfig(
        UserAgent(appName, appVersion, appUrl),
        OAuthEnabled(BasicSignature(Consumer(key, secret)))
      )

      implicit override def ec: ExecutionContext = executionContext
    }

  def oAuthClientWith[Version <: OAuthVersion](
    key: String,
    secret: String,
    token: Token,
    appName: String = "someApp",
    appVersion: Option[String] = Some("1.0"),
    appUrl: Option[String] = Some("app.git")
  ): IOAuthClient[Version] = {
    val userAgent: UserAgent = UserAgent(appName, appVersion, appUrl)
    val consumer: Consumer = Consumer(key, secret)
    new IOAuthClient(userAgent, V1.AccessToken(token, consumer))
  }
}
