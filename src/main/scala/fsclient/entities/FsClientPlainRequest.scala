package fsclient.entities

import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s.{Method, Uri}

trait FsClientPlainRequest[Response] {
  def uri: Uri
  def method: Method
}

object FsClientPlainRequest {
  trait GET[Response] extends FsClientPlainRequest[Response] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait POST[Response] extends FsClientPlainRequest[Response] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}

sealed trait FsClientRequestWithBody[Body, Response] extends FsClientPlainRequest[Response] {
  def body: Body
}

object FsClientRequestWithBody {
  trait GET[Body, Response] extends FsClientRequestWithBody[Body, Response] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait POST[Body, Response] extends FsClientRequestWithBody[Body, Response] {
    override val method: DefaultMethodWithBody = Method.POST
  }
}

trait AccessTokenRequest extends FsClientPlainRequest[AccessToken] {
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
