package fsclient.entities

sealed trait OAuthInfo

case object OAuthDisabled extends OAuthInfo
case class OAuthEnabled[V <: OAuthVersion](signer: Signer[V]) extends OAuthInfo
