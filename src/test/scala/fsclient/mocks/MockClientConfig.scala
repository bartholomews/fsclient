package fsclient.mocks

import fsclient.client.io_client.{IOAuthClient, IOClient}
import fsclient.config.AppConsumer
import fsclient.entities.OAuthVersion.OAuthV1.AccessTokenV1
import fsclient.entities.{OAuthToken, OAuthVersion}
import org.http4s.client.oauth1.{Consumer, Token}
import org.scalatest.Assertions._

import scala.concurrent.ExecutionContext

trait MockClientConfig {

  implicit val ec: ExecutionContext = ExecutionContext.global

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

  def validSimpleClient(): IOClient =
    simpleClientWith(validConsumerKey, validConsumerSecret)

  def validOAuthClient(oAuthVersion: OAuthVersion): IOAuthClient =
    oAuthVersion match {
      case OAuthVersion.OAuthV1 =>
        oAuthClientWith(validConsumerKey, validConsumerSecret, validToken)

      case OAuthVersion.OAuthV2 =>
        fail("OAuthV2 client: test setup not implemented")
    }

  def simpleClientWith(key: String,
                       secret: String,
                       appName: String = "someApp",
                       appVersion: Option[String] = Some("1.0"),
                       appUrl: Option[String] = Some("app.git")): IOClient =
    new IOClient(AppConsumer(appName, appVersion, appUrl, key, secret))

  def oAuthClientWith(key: String,
                      secret: String,
                      token: Token,
                      appName: String = "someApp",
                      appVersion: Option[String] = Some("1.0"),
                      appUrl: Option[String] = Some("app.git")): IOAuthClient = {

    val appConsumer: AppConsumer = AppConsumer(appName, appVersion, appUrl, key, secret)
    implicit val consumer: Consumer = Consumer(appConsumer.key, appConsumer.secret)
    implicit val oAuthToken: OAuthToken = AccessTokenV1(token)
    new IOAuthClient(appConsumer)
  }
}
