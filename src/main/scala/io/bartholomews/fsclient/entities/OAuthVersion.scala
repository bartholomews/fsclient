package io.bartholomews.fsclient.entities

sealed trait OAuthVersion

object OAuthVersion {
  type OAuthV1 = OAuthVersion.V1.type
  type OAuthV2 = OAuthVersion.V2.type

  // https://tools.ietf.org/html/rfc5849 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  case object V1 extends OAuthVersion

  // https://tools.ietf.org/html/rfc6749 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  case object V2 extends OAuthVersion
}
