package fsclient.entities

import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{Method, Uri}

sealed trait HttpRequest[Response] {
  def uri: Uri

  def method: Method
}

sealed trait HttpRequestWithBody[Body, Response] extends HttpRequest[Response] {
  def body: Body
}

object HttpRequest {
  case class GET[Response](uri: Uri) extends HttpRequest[Response] {
    override val method: SafeMethodWithBody = Method.GET
  }
  case class POST[Body, Response](uri: Uri, body: Body) extends HttpRequestWithBody[Body, Response] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}

sealed trait AccessTokenRequest extends HttpRequest[AccessToken] {
  def token: OAuthToken
}

// FIXME: If this is not a standard oAuth request, should be constructed client-side
object AccessTokenRequest {
  def apply(requestUri: Uri, requestToken: RequestToken): AccessTokenRequest =
    new AccessTokenRequest {
      override val token = OAuthToken(requestToken)
      override val uri: Uri = requestUri
      override val method: Method = Method.POST
    }
}
