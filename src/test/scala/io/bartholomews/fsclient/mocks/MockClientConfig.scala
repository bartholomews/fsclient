package io.bartholomews.fsclient.mocks

import cats.effect.{ContextShift, IO}
import io.bartholomews.fsclient.client.{FsClient, FsClientV1}
import io.bartholomews.fsclient.config.UserAgent
import io.bartholomews.fsclient.entities.oauth.OAuthVersion.OAuthV1
import io.bartholomews.fsclient.entities.oauth.{AccessTokenCredentials, OAuthVersion}
import org.http4s.client.oauth1.{Consumer, Token}
import org.scalatest.Assertions._

import scala.concurrent.ExecutionContext

trait MockClientConfig {

  implicit val executionContext: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(executionContext)

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

  def validSimpleClient(): FsClient[IO, OAuthV1] =
    simpleClientWith(validConsumerKey, validConsumerSecret)

  def validOAuthClient[V <: OAuthVersion](authVersion: V): FsClient[IO, OAuthV1] =
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
  ): FsClient[IO, OAuthV1] =
    FsClientV1.basic(
      UserAgent(appName, appVersion, appUrl),
      Consumer(key, secret)
    )

  def oAuthClientWith(
    key: String,
    secret: String,
    token: Token,
    appName: String = "someApp",
    appVersion: Option[String] = Some("1.0"),
    appUrl: Option[String] = Some("app.git")
  ): FsClient[IO, OAuthV1] = {
    val userAgent: UserAgent = UserAgent(appName, appVersion, appUrl)
    val consumer: Consumer = Consumer(key, secret)
    FsClientV1(userAgent, AccessTokenCredentials(token, consumer))
  }
}
