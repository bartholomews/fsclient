package fsclient.requests

import fsclient.entities.OAuthVersion.Version1
import org.http4s.Uri

object OAuthV1AuthorizationFramework {
  // FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
  case class AccessTokenRequest(uri: Uri) extends FsAuthRequest.Post[Nothing, String, Version1.AccessTokenResponse] {
    final override val body: Option[Nothing] = None
  }
}
