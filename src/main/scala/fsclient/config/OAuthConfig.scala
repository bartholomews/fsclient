package fsclient.config

object OAuthConfig {

  lazy val oAuthConsumer: OAuthConsumer = pureconfig.loadConfigOrThrow[Config].oauth.consumer

  private case class Config(oauth: OAuthConfig)

  private case class OAuthConfig(consumer: OAuthConsumer)
}
