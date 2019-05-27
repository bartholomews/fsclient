package fsclient.mocks

import fsclient.config.OAuthConsumer
import fsclient.entities.OAuthAccessToken
import fsclient.http.client.IOClient
import org.http4s.client.oauth1.Token

trait MockClientConfig {

  import scala.concurrent.ExecutionContext.Implicits.global

  final val validConsumerKey = "VALID_CONSUMER_KEY"
  final val validConsumerSecret = "VALID_CONSUMER_SECRET"

  final val invalidConsumerKey = "INVALID_CONSUMER_KEY"
  final val invalidConsumerSecret = "INVALID_CONSUMER_SECRET"

  final val validOAuthTokenValue = "VALID_OAUTH_TOKEN_VALUE"
  final val validOAuthTokenSecret = "VALID_OAUTH_TOKEN_SECRET"
  final val validOAuthVerifier = "OAUTH_VERIFIER"

  final val validOAuthAccessToken: OAuthAccessToken = new OAuthAccessToken {
    override val token: Token = Token(validOAuthTokenValue, validOAuthTokenSecret)
    override val verifier: Option[String] = Some(validOAuthVerifier)
  }

  def validSimpleClient: IOClient = clientWith(validConsumerKey, validConsumerSecret, None)

  def validOAuthClient: IOClient = clientWith(validConsumerKey, validConsumerSecret, Some(validOAuthAccessToken))

  def clientWith(key: String,
                 secret: String,
                 accessToken: Option[OAuthAccessToken],
                 appName: String = "someApp",
                 appVersion: Option[String] = Some("1.0"),
                 appUrl: Option[String] = Some("app.git")): IOClient = new IOClient(
    OAuthConsumer(appName, appVersion, appUrl, key, secret),
    accessToken
  )
}