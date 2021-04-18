package io.bartholomews.scalatestudo.data

import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, Token}
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken}
import io.bartholomews.fsclient.core.oauth.v2.{ClientId, ClientPassword, ClientSecret}
import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner, RedirectUri, Scope}
import sttp.client3.UriContext

object ClientData {

  val sampleUserAgent: UserAgent = UserAgent(
    appName = "SAMPLE_APP_NAME",
    appVersion = Some("SAMPLE_APP_VERSION"),
    appUrl = Some("https://bartholomews.io/sample-app-url")
  )

  val sampleRedirectUri: RedirectUri = RedirectUri(uri"https://bartholomews.io/callback")

  object v1 {
    val sampleToken: Token = Token(value = "SAMPLE_TOKEN_VALUE", secret = "SAMPLE_TOKEN_SECRET")
    val sampleConsumer: Consumer = Consumer(
      key = "SAMPLE_CONSUMER_KEY",
      secret = "SAMPLE_CONSUMER_SECRET"
    )
  }

  object v2 {
    val sampleClientId: ClientId = ClientId("SAMPLE_CLIENT_ID")
    val sampleClientSecret: ClientSecret = ClientSecret("SAMPLE_CLIENT_SECRET")
    val sampleClientPassword: ClientPassword = ClientPassword(sampleClientId, sampleClientSecret)
    val sampleRefreshToken: RefreshToken = RefreshToken("SAMPLE_REFRESH_TOKEN")
    val sampleAccessTokenKey: AccessToken = AccessToken(
      "00000000000-0000000000000000000-0000000-0000000000000000000000000000000000000000001"
    )
    val sampleAccessTokenSigner: AccessTokenSigner = AccessTokenSigner(
      generatedAt = 21312L,
      accessToken = sampleAccessTokenKey,
      tokenType = "bearer",
      expiresIn = 1000L,
      refreshToken = Some(sampleRefreshToken),
      scope = Scope(List.empty)
    )

    val sampleNonRefreshableToken: NonRefreshableTokenSigner = NonRefreshableTokenSigner(
      generatedAt = 21312L,
      accessToken = sampleAccessTokenKey,
      tokenType = "bearer",
      expiresIn = 1000L,
      scope = Scope(List.empty)
    )
  }
}
