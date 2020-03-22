package fsclient.entities

sealed trait OAuthInfo

case object OAuthDisabled extends OAuthInfo
case class OAuthEnabled[V <: OAuthVersion](signer: Signer[V]) extends OAuthInfo

object OAuthInfo {
  type OAuthV1 = OAuthEnabled[OAuthVersion.V1.type]
  type OAuthV2 = OAuthEnabled[OAuthVersion.V2.type]
}