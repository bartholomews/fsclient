package fsclient.entities

import cats.effect.Effect
import fsclient.utils.Logger._
import io.circe.Encoder
import org.http4s.Method.{DefaultMethodWithBody, SafeMethodWithBody}
import org.http4s._

trait FsClientPlainRequest {
  def uri: Uri
  def method: Method
  def headers: Headers = Headers.empty
  def toHttpRequest[F[_]: Effect]: Request[F] =
    logRequest {
      Request()
        .withHeaders(headers)
        .withMethod(method)
        .withUri(uri)
    }
}

object FsClientPlainRequest {
  trait Get extends FsClientPlainRequest {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post extends FsClientPlainRequest {
    override val method: DefaultMethodWithBody = Method.POST
  }
}

trait AccessTokenRequest extends FsClientPlainRequest {
  def token: OAuthToken
}

trait FsClientRequestWithBody[Body] {
  def uri: Uri
  def method: Method
  def headers: Headers = Headers.empty
  def body: Body
  def toHttpRequest[F[_]: Effect](implicit requestBodyEncoder: EntityEncoder[F, Body]): Request[F] =
    logRequest(
      body,
      Request()
        .withMethod(method)
        .withUri(uri)
        .withEntity(body)
        .withHeaders(headers)
    )
}

object FsClientRequestWithBody {
  trait Get[Body] extends FsClientRequestWithBody[Body] {
    override val method: SafeMethodWithBody = Method.GET
  }
  trait Post[Body] extends FsClientRequestWithBody[Body] {
    override val method: DefaultMethodWithBody = Method.POST
  }
  trait PostJson[Body] extends FsClientRequestWithBody[Body] {
    override val method: DefaultMethodWithBody = Method.POST
    import org.http4s.circe._
    implicit def entityJsonEncoder[F[_]: Effect](implicit encoder: Encoder[Body]): EntityEncoder[F, Body] =
      jsonEncoderOf[F, Body]
  }
}

// FIXME: If this is not a standard oAuth request, should be constructed client-side
object AccessTokenRequest {
  def apply(requestUri: Uri, requestToken: RequestToken): AccessTokenRequest =
    new AccessTokenRequest {
      override val token = OAuthToken(requestToken.token, Some(requestToken.verifier))
      override val uri: Uri = requestUri
      override val method: Method = Method.POST
    }
}
