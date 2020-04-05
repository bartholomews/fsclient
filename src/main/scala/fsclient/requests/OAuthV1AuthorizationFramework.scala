package fsclient.requests

import fsclient.entities.OAuthVersion.Version1.AccessTokenV1
import org.http4s.Uri

object OAuthV1AuthorizationFramework {

  sealed trait SignerType
  case object OAuthV1BasicSignature extends SignerType
  case object OAuthV1AccessToken extends SignerType

  // FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
  case class AccessTokenRequest(uri: Uri) extends FsAuthRequest.Post[Nothing, String, AccessTokenV1] {
    final override val body: Option[Nothing] = None
  }
}
