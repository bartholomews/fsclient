package io.bartholomews.fsclient.config

import io.bartholomews.fsclient.entities.oauth.{AuthDisabled, ClientCredentials, Signer, SignerV1}
import org.http4s.client.oauth1.Consumer
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderException

import scala.reflect.ClassTag

case class FsClientConfig[S <: Signer](userAgent: UserAgent, signer: S)

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

    def basic(userAgent: UserAgent, consumer: Consumer): FsClientConfig[SignerV1] =
      FsClientConfig(userAgent, ClientCredentials(consumer))

    def basic(consumerNamespace: String): Result[FsClientConfig[SignerV1]] =
      for {
        consumer <- ConfigSource.default
          .at(consumerNamespace)
          .at(namespace = "consumer")
          .load[Consumer]

        userAgent <- ConfigSource.default.at(namespace = "user-agent").load[UserAgent]

      } yield v1.basic(userAgent, consumer)
  }

  def disabled(userAgent: UserAgent): FsClientConfig[AuthDisabled.type] = FsClientConfig(userAgent, AuthDisabled)

  private[fsclient] case class LoggerConfig(name: String)
}
