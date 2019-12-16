package fsclient.requests

import fsclient.oauth.OAuthToken

sealed trait OAuthInfo

case object OAuthDisabled extends OAuthInfo

case class OAuthEnabled(token: OAuthToken) extends OAuthInfo
