package fsclient.requests

import cats.effect.Effect
import fsclient.conf.UserAgent
import fsclient.utils.{FsHeaders, Logger}
import org.http4s._
import org.http4s.client.oauth1.Consumer
import Logger._

// FIXME: Find a good way to unify these combinations: `Simple/Auth`
private[fsclient] trait FsClientRequest[Body] {
  def uri: Uri

  def method: Method

  def headers: Headers = Headers.empty

  private[fsclient] def body: Option[Body]

  final private[fsclient] def toHttpRequest[F[_]: Effect](
    userAgent: UserAgent
  )(implicit requestBodyEncoder: EntityEncoder[F, Body]): Request[F] =
    logRequest {
      body
        .fold[Request[F]](Request())(b => Request().withEntity(logRequestBody(b)))
        .withMethod(method)
        .withUri(uri)
        .withHeaders(headers.++(Headers.of(FsHeaders.userAgent(userAgent.value))))
    }
}

import fsclient.entities.AuthVersion._

trait AccessTokenRequestV1 extends FsClientRequest[Nothing] {
  def token: V1.OAuthToken
}

// FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
object AccessTokenRequestV1 {
  def apply(requestUri: Uri, requestToken: V1.RequestToken)(implicit consumer: Consumer): AccessTokenRequestV1 =
    new AccessTokenRequestV1 {
      final override val token: V1.OAuthToken = V1.RequestToken(requestToken.token, requestToken.verifier, consumer)
      final override val uri: Uri = requestUri
      final override val method: Method = Method.POST
      final override val body: Option[Nothing] = None
    }
}
