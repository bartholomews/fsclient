package fsclient.entities

import fsclient.oauth.OAuthVersion.{OAuthV1, OAuthV2}
import fsclient.oauth.{OAuthToken, OAuthTokenV1, OAuthTokenV2, OAuthVersion}

sealed trait OAuthInfo

case object OAuthDisabled extends OAuthInfo

sealed trait OAuth extends OAuthInfo {
  def oAuthVersion: OAuthVersion
}

object OAuth {
  def apply(token: OAuthToken): OAuth = token match {
    case v1: OAuthTokenV1 => OAuthVersion1(v1)
    case v2: OAuthTokenV2 => OAuthVersion2(v2)
  }
}

case class OAuthVersion1(oAuthToken: OAuthTokenV1) extends OAuth {
  override val oAuthVersion: OAuthVersion.OAuthV1.type = OAuthV1
}

case class OAuthVersion2(oAuthToken: OAuthTokenV2) extends OAuth {
  override val oAuthVersion: OAuthVersion.OAuthV2.type = OAuthV2
}
