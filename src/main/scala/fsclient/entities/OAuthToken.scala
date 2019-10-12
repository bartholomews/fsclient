package fsclient.entities

import org.http4s.client.oauth1.Token

trait OAuthToken {
  def token: Token

  def verifier: Option[String]
}

object OAuthToken {
  def apply(requestToken: RequestToken): OAuthToken = new OAuthToken {
    override val token: Token = requestToken.token
    override val verifier: Option[String] = Some(requestToken.verifier)
  }

  def apply(accessToken: AccessToken): OAuthToken = new OAuthToken {
    override val token: Token = accessToken.token
    override val verifier: Option[String] = None
  }
}

case class RequestToken(token: Token, verifier: String)

case class AccessToken(token: Token)
