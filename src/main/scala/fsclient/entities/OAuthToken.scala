package fsclient.entities

import org.http4s.client.oauth1.Token

trait OAuthToken {
  def token: Token

  def verifier: Option[String]
}

case class RequestToken(token: Token, verifier: Option[String]) extends OAuthToken

case class AccessToken(token: Token) extends OAuthToken {
  override val verifier: Option[String] = None
}