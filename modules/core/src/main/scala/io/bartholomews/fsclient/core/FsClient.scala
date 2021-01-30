package io.bartholomews.fsclient.core

import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.Consumer
import io.bartholomews.fsclient.core.oauth.{ClientCredentials, ClientPasswordAuthentication, Signer, SignerV1}
import pureconfig.ConfigReader.Result
import pureconfig.ConfigSource
import sttp.client3.SttpBackend

case class FsClient[F[_], S <: Signer](userAgent: UserAgent, signer: S, backend: SttpBackend[F, Any])

object FsClient {

  /**
   * Helpers to load consumer info and signer from application config.
   *
   * OAuth V1 Basic signature
   *
   * consumer {
   *   app-name: "<consumer-app-name>",
   *   app-version: "<consumer-app-version>",
   *   app-url: "<consumer-app-url>", // OPTIONAL
   *   key: "<consumer-key>",
   *   secret: "<consumer-secret>"
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
   */
  object v1 {

    import pureconfig.generic.auto._

    def clientCredentials[F[_]](userAgent: UserAgent, consumer: Consumer)(
      backend: SttpBackend[F, Any]
    ): FsClient[F, SignerV1] = FsClient(userAgent, ClientCredentials(consumer), backend)

    def clientCredentials[F[_]](
      consumerNamespace: String
    )(backend: SttpBackend[F, Any]): Result[FsClient[F, SignerV1]] =
      for {
        consumer <-
          ConfigSource.default
            .at(consumerNamespace)
            .at(namespace = "consumer")
            .load[Consumer]

        userAgent <- ConfigSource.default.at(namespace = "user-agent").load[UserAgent]

      } yield clientCredentials(userAgent, consumer)(backend)
  }

  object v2 {
    def clientPassword[F[_]](
      userAgent: UserAgent,
      clientPasswordAuthentication: ClientPasswordAuthentication
    )(backend: SttpBackend[F, Any]): FsClient[F, ClientPasswordAuthentication] =
      FsClient(userAgent = userAgent, signer = clientPasswordAuthentication, backend)
  }
}
