package fsclient.entities
import org.http4s.{Method, Uri}

sealed trait AccessTokenRequest extends HttpRequest[None.type, AccessToken] {
  def token: OAuthToken
}

object AccessTokenRequest {
  def apply(requestUri: Uri, requestToken: RequestToken): AccessTokenRequest =
    new AccessTokenRequest {
      override val token = OAuthToken(requestToken)
      override val uri: Uri = requestUri
      override val method: Method = Method.POST
      override val body
        : None.type = None // OAuthToken = OAuthToken(requestToken)
    }
}
