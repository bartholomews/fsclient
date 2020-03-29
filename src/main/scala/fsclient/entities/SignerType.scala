package fsclient.entities

sealed trait SignerType

object SignerType {
  case object OAuthV1BasicSignature extends SignerType
  case object OAuthV1AccessToken extends SignerType
}
