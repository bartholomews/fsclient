package fsclient.entities

import fsclient.oauth.OAuthVersion
import org.http4s.client.oauth1.Token

sealed trait OAuthInfo {
  def oAuthVersion: Option[OAuthVersion]
}

case class Basic(oAuthVersion: Option[OAuthVersion]) extends OAuthInfo

case class OAuthTokenInfo private (oAuthVersion: Option[OAuthVersion], oAuthToken: OAuthToken) extends OAuthInfo
object OAuthTokenInfo {
  def apply(requestToken: RequestToken, oAuthVersion: Option[OAuthVersion]): OAuthTokenInfo = new OAuthTokenInfo(
    oAuthVersion,
    OAuthToken(requestToken.token, Some(requestToken.verifier))
  )
  def apply(accessToken: AccessToken, oAuthVersion: Option[OAuthVersion]): OAuthTokenInfo = new OAuthTokenInfo(
    oAuthVersion,
    OAuthToken(accessToken.token, verifier = None)
  )
}

case class OAuthToken(token: Token, verifier: Option[String])
case class RequestToken(token: Token, verifier: String)
case class AccessToken(token: Token)
