package io.bartholomews.fsclient.config

import io.bartholomews.fsclient.entities.oauth.OAuthVersion.OAuthV1
import io.bartholomews.fsclient.entities.oauth.{AuthDisabled, ClientCredentials, OAuthVersion, Signer}
import org.http4s.client.oauth1.{Consumer, Token}
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

case class FsClientConfig[+V <: OAuthVersion](userAgent: UserAgent, signer: Signer[V])

/**
 * Helpers to load consumer info and signer from application config.
 *
 * OAuth V1 Basic signature
 *
 * consumer {
 *   app-name: "<consumer-app-name>",
 *   app-version: "<consumer-app-version>",
 *   app-url: "<consumer-app-url>", // OPTIONAL
 * key: "<consumer-key>",
 *   secret: "<consumer-secret>"
 * }
 *
 * logger {
 *   name = "<app-logger-name>" // OPTIONAL, DEFAULT WITH `fsclient` LOGGER
 * }
 *
 * OAuth V1 Token needs additional token information:
 *
 * access-token {
 *   value = "<token-value>"
 *   secret = "<token-secret>"
 * }
 *
 * There are constructors with string `key` parameter allow to easily have the config under an object key:
 *
 * my-app {
 *   consumer...
 * }
 *
 */
object FsClientConfig {

  implicit class LoadConfigOrThrow[A: ClassTag](maybeConfig: Result[A]) {
    def orThrow: A = {
      import cats.syntax.either._
      maybeConfig.valueOr(failures => throw new ConfigReaderException[A](failures))
    }
  }

  import pureconfig.generic.auto._

  object v1 {

    private def basic(consumerConfig: ConsumerConfig) =
      new FsClientConfig(
        consumerConfig.userAgent,
        ClientCredentials(consumerConfig.consumer)
      )

    def basic(userAgent: UserAgent, consumer: Consumer): FsClientConfig[OAuthV1] = {
      val signer: Signer[OAuthV1] = ClientCredentials(consumer)
      FsClientConfig[OAuthV1](userAgent, signer)
    }

    def basic(key: String): Result[FsClientConfig[OAuthV1]] =
      ConfigSource.default
        .load[BasicAppConfig](Derivations.withCustomKey(key))
        .map(_.consumer)
        .map(v1.basic)

    def basic(): Result[FsClientConfig[OAuthV1]] =
      ConfigSource.default
        .load[BasicAppConfig]
        .map(_.consumer)
        .map(v1.basic)
  }

  def disabled(userAgent: UserAgent): FsClientConfig[Nothing] = FsClientConfig(userAgent, AuthDisabled)

  sealed trait AppConfig
  case class BasicAppConfig(consumer: ConsumerConfig) extends AppConfig
  case class TokenAppConfig(consumer: ConsumerConfig, accessToken: Token) extends AppConfig

  case class ConsumerConfig(
    appName: String,
    appVersion: Option[String],
    appUrl: Option[String],
    key: String,
    secret: String
  ) {
    def userAgent: UserAgent = UserAgent(appName, appVersion, appUrl)
    def consumer: Consumer = Consumer(key, secret)
  }

  private[fsclient] case class LoggerConfig(name: String)
}
