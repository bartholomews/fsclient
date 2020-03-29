package fsclient.config

import fsclient.entities.OAuthInfo.OAuthV1
import fsclient.entities.OAuthVersion.V1
import fsclient.entities._
import org.http4s.client.oauth1.{Consumer, Token}
import pureconfig.error.{ConfigReaderException, ConfigReaderFailures}
import pureconfig.{ConfigReader, ConfigSource, Derivation}

import scala.reflect.ClassTag

case class FsClientConfig[A <: OAuthInfo](userAgent: UserAgent, authInfo: A)

object FsClientConfig {

  implicit class LoadConfigOrThrow[A <: OAuthInfo: ClassTag](
    maybeConfig: Either[ConfigReaderFailures, FsClientConfig[A]]
  ) {
    def orThrow: FsClientConfig[A] = {
      import cats.syntax.either._
      maybeConfig.valueOr(failures => throw new ConfigReaderException[A](failures))
    }
  }

  import pureconfig.generic.auto._

  object v1 {

    private def apply(consumerConfig: ConsumerConfig, signer: Signer[V1.type]) = new FsClientConfig(
      UserAgent(consumerConfig.appName, consumerConfig.appVersion, consumerConfig.appUrl),
      OAuthEnabled(signer)
    )

    private def basic(consumerConfig: ConsumerConfig) =
      apply(consumerConfig, V1.BasicSignature(consumerConfig.consumer))

    def basic(userAgent: UserAgent, consumer: Consumer): FsClientConfig[OAuthV1] = {
      val signer: Signer[V1.type] = V1.BasicSignature(consumer)
      FsClientConfig(userAgent, OAuthEnabled(signer))
    }

    def basic(key: String): Either[ConfigReaderFailures, FsClientConfig[OAuthEnabled[OAuthVersion.V1.type]]] = {
      implicit val customConfigReader: Derivation[ConfigReader[Config]] = Derivations.withCustomKey(key)
      ConfigSource.default
        .load[Config]
        .map(_.consumer)
        .map(v1.basic)
    }

    def basic(): Either[ConfigReaderFailures, FsClientConfig[OAuthEnabled[OAuthVersion.V1.type]]] =
      ConfigSource.default
        .load[Config]
        .map(_.consumer)
        .map(v1.basic)

    private def token(consumerConfig: ConsumerConfig, token: Token) =
      apply(consumerConfig, V1.AccessToken(token, consumerConfig.consumer))

    def token(userAgent: UserAgent, accessToken: V1.AccessToken): FsClientConfig[OAuthV1] =
      FsClientConfig(userAgent, OAuthEnabled(accessToken))

    def token(key: String): Either[ConfigReaderFailures, FsClientConfig[OAuthEnabled[OAuthVersion.V1.type]]] =
      for {
        config <- ConfigSource.default.load[Config](Derivations.withCustomKey(key))
        token <- ConfigSource.default.load[AccessTokenConfig](Derivations.withCustomKey(key))
      } yield v1.token(config.consumer, token.accessToken)

    def token(): Either[ConfigReaderFailures, FsClientConfig[OAuthEnabled[OAuthVersion.V1.type]]] =
      for {
        config <- ConfigSource.default.load[Config]
        token <- ConfigSource.default.load[AccessTokenConfig]
      } yield v1.token(config.consumer, token.accessToken)
  }

  def disabled(userAgent: UserAgent): FsClientConfig[OAuthDisabled.type] = FsClientConfig(userAgent, OAuthDisabled)

  private[fsclient] case class Config(consumer: ConsumerConfig, logger: LoggerConfig)

  private[fsclient] case class ConsumerConfig(
    appName: String,
    appVersion: Option[String],
    appUrl: Option[String],
    key: String,
    secret: String
  ) {

    def consumer: Consumer = Consumer(key, secret)
  }

  private[fsclient] case class AccessTokenConfig(accessToken: Token)

  private[fsclient] case class LoggerConfig(name: String)
}
