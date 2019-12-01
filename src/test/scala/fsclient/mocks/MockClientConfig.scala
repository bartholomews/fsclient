package fsclient.mocks

import fsclient.config.OAuthConsumer
import fsclient.http.client.{IOAuthClient, IOClient}
import fsclient.oauth.OAuthVersion.OAuthV1.AccessTokenV1
import fsclient.oauth.{OAuthToken, OAuthVersion}
import org.http4s.client.oauth1.Token
import org.scalatest.Assertions._

import scala.concurrent.ExecutionContext

trait MockClientConfig {

  implicit val ec: ExecutionContext = ExecutionContext.global

  final val validConsumerKey = "VALID_CONSUMER_KEY"
  final val validConsumerSecret = "VALID_CONSUMER_SECRET"

  final val invalidConsumerKey = "INVALID_CONSUMER_KEY"
  final val invalidConsumerSecret = "INVALID_CONSUMER_SECRET"

  final val validOAuthTokenValue = "VALID_OAUTH_TOKEN_VALUE"
  final val validOAuthTokenSecret = "VALID_OAUTH_TOKEN_SECRET"
  final val validOAuthVerifier = "OAUTH_VERIFIER"

  final val validToken = Token(validOAuthTokenValue, validOAuthTokenSecret)

  final val validOAuthAccessTokenV1: AccessTokenV1 = AccessTokenV1(
    Token(validOAuthTokenValue, validOAuthTokenSecret)
  )
  // override val verifier: Option[String] = Some(validOAuthVerifier)

  def validSimpleClient(): IOClient =
    simpleClientWith(validConsumerKey, validConsumerSecret)

  def validOAuthClient(oAuthVersion: OAuthVersion): IOAuthClient =
    oAuthVersion match {
      case OAuthVersion.OAuthV1 =>
        oAuthClientWith(validConsumerKey, validConsumerSecret, validOAuthAccessTokenV1)

      case OAuthVersion.OAuthV2 =>
        fail("OAuthV2 client: test setup not implemented")
    }

  def simpleClientWith(key: String,
                       secret: String,
                       appName: String = "someApp",
                       appVersion: Option[String] = Some("1.0"),
                       appUrl: Option[String] = Some("app.git")): IOClient =
    new IOClient(OAuthConsumer(appName, appVersion, appUrl, key, secret))

  def oAuthClientWith(key: String,
                      secret: String,
                      accessToken: OAuthToken,
                      appName: String = "someApp",
                      appVersion: Option[String] = Some("1.0"),
                      appUrl: Option[String] = Some("app.git")): IOAuthClient = {

    implicit val token: OAuthToken = accessToken
    new IOAuthClient(OAuthConsumer(appName, appVersion, appUrl, key, secret))
  }
}
