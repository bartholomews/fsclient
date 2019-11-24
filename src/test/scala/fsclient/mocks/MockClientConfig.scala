package fsclient.mocks

import fsclient.config.OAuthConsumer
import fsclient.entities.AccessToken
import fsclient.http.client.{IOAuthClient, IOClient}
import fsclient.oauth.OAuthVersion
import org.http4s.client.oauth1.Token

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

  final val validOAuthAccessToken: AccessToken = AccessToken(
    Token(validOAuthTokenValue, validOAuthTokenSecret)
  )
  // override val verifier: Option[String] = Some(validOAuthVerifier)

  def validSimpleClient(oAuthVersion: OAuthVersion): IOClient =
    simpleClientWith(oAuthVersion, validConsumerKey, validConsumerSecret)

  def validOAuthClient(oAuthVersion: OAuthVersion): IOAuthClient =
    oAuthClientWith(oAuthVersion, validConsumerKey, validConsumerSecret, validOAuthAccessToken)

  def simpleClientWith(oAuthVersion: OAuthVersion,
                       key: String,
                       secret: String,
                       appName: String = "someApp",
                       appVersion: Option[String] = Some("1.0"),
                       appUrl: Option[String] = Some("app.git")): IOClient =
    new IOClient(OAuthConsumer(appName, appVersion, appUrl, key, secret), oAuthVersion)

  def oAuthClientWith(oAuthVersion: OAuthVersion,
                      key: String,
                      secret: String,
                      accessToken: AccessToken,
                      appName: String = "someApp",
                      appVersion: Option[String] = Some("1.0"),
                      appUrl: Option[String] = Some("app.git")): IOAuthClient = {

    implicit val token: AccessToken = accessToken
    new IOAuthClient(OAuthConsumer(appName, appVersion, appUrl, key, secret), oAuthVersion)
  }
}
