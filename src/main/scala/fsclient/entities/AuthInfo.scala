package fsclient.entities

sealed trait AuthInfo

case object AuthDisabled extends AuthInfo
case class AuthEnabled(signer: Signer) extends AuthInfo
