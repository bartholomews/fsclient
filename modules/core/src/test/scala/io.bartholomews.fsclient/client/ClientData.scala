package io.bartholomews.fsclient.client

import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.Consumer
import io.bartholomews.fsclient.core.oauth.v2.{AuthorizationCode, ClientId, ClientPassword, ClientSecret}
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{RedirectUri, RefreshToken}
import sttp.client3.UriContext

object ClientData {

  val sampleUserAgent: UserAgent = UserAgent(
    appName = "SAMPLE_APP_NAME",
    appVersion = Some("SAMPLE_APP_VERSION"),
    appUrl = Some("https://bartholomews.io/sample-app-url")
  )

  val sampleRedirectUri: RedirectUri = RedirectUri(uri"https://bartholomews.io/callback")

  val sampleAuthorizationCode: AuthorizationCode = AuthorizationCode("SAMPLE_AUTHORIZATION_CODE")

  val sampleRefreshToken: RefreshToken = RefreshToken("SAMPLE_REFRESH_TOKEN")

  val sampleClientPassword: ClientPassword = ClientPassword(
    clientId = ClientId("SAMPLE_CLIENT_ID"),
    clientSecret = ClientSecret("SAMPLE_CLIENT_SECRET")
  )

  val sampleConsumer: Consumer =
    Consumer(key = "SAMPLE_CONSUMER_KEY", secret = "SAMPLE_CONSUMER_SECRET")
}
