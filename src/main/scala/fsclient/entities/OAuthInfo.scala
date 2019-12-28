package fsclient.entities

sealed trait OAuthInfo

case object OAuthDisabled extends OAuthInfo

case class OAuthEnabled(token: OAuthToken) extends OAuthInfo
