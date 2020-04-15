package io.bartholomews.fsclient.entities.v2

import io.bartholomews.fsclient.codecs.FsJsonResponsePipe
import io.bartholomews.fsclient.entities.defaultConfig
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.{AccessToken, RefreshToken}
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

// https://tools.ietf.org/html/rfc6749#section-5.1
case class AccessTokenV2(
  accessToken: AccessToken,
  // https://tools.ietf.org/html/rfc6749#section-7.1
  tokenType: String,
  expiresIn: Option[Long], // RECOMMENDED.  The lifetime in seconds of the access token.
  refreshToken: Option[RefreshToken],
  // https://tools.ietf.org/html/rfc6749#section-3.3
  scope: Option[String]
) {
  private val generatedAt: Long = System.currentTimeMillis()
  def isExpired: Option[Boolean] =
    expiresIn.map(expInSec => System.currentTimeMillis() > generatedAt + (expInSec * 1000))
}

object AccessTokenV2 extends FsJsonResponsePipe[AccessTokenV2] {
  implicit val decoder: Decoder[AccessTokenV2] = deriveConfiguredDecoder
}
