package fsclient.entities

sealed trait OAuthInfo

case object OAuthDisabled extends OAuthInfo
case class OAuthEnabled(signer: Signer) extends OAuthInfo
