package fsclient.config

import pureconfig.ConfigSource

object OAuthConfig {

  import pureconfig.generic.auto._

  lazy val oAuthConsumer: OAuthConsumer =
    ConfigSource.default.loadOrThrow[Config].oauth.consumer

  private case class Config(oauth: OAuthConfig)

  private case class OAuthConfig(consumer: OAuthConsumer)

}
