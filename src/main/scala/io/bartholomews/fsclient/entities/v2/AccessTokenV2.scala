package io.bartholomews.fsclient.entities.v2

import io.bartholomews.fsclient.codecs.FsJsonResponsePipe
import io.bartholomews.fsclient.entities.defaultConfig
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.{AccessToken, RefreshToken}
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder

// https://tools.ietf.org/html/rfc6749#section-5.1
sealed trait AccessTokenV2 {
  final private val generatedAt: Long = System.currentTimeMillis()
  def accessToken: AccessToken
  // https://tools.ietf.org/html/rfc6749#section-7.1
  def tokenType: String
  def expiresIn: Long
  def maybeRefreshToken: Option[RefreshToken]
  // https://tools.ietf.org/html/rfc6749#section-3.3
  def scope: Option[String]
  final def isExpired: Boolean =
    System.currentTimeMillis() > generatedAt + (expiresIn * 1000)
}

/*
 * https://tools.ietf.org/html/rfc6749#section-4.1.2
 * Refreshable user-level token
 */
case class AuthorizationCode(
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  refreshToken: RefreshToken,
  scope: Option[String]
) extends AccessTokenV2 {
  final override val maybeRefreshToken: Option[RefreshToken] =
    Some(refreshToken)
}

object AuthorizationCode extends FsJsonResponsePipe[AuthorizationCode] {
  implicit val decoder: Decoder[AuthorizationCode] = deriveConfiguredDecoder
}

/*
 * Non-refreshable token
 *  user-level implicit grant (https://tools.ietf.org/html/rfc6749#section-4.2.2)
 *  server-level client credentials (https://tools.ietf.org/html/rfc6749#section-4.4.3)
 */
case class NonRefreshableToken(
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  scope: Option[String]
) extends AccessTokenV2 {
  final override val maybeRefreshToken: Option[RefreshToken] = None
}

object NonRefreshableToken extends FsJsonResponsePipe[NonRefreshableToken] {
  implicit val decoder: Decoder[NonRefreshableToken] = deriveConfiguredDecoder
}
