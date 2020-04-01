package fsclient.requests

import cats.effect.Effect
import fsclient.config.UserAgent
import fsclient.utils.FsHeaders
import fsclient.utils.Logger._
import org.http4s._

// FIXME: Find a good way to unify these combinations: `Simple/Auth`
private[fsclient] trait FsClientRequest[Body] {
  def uri: Uri

  def headers: Headers = Headers.empty

  private[fsclient] def method: Method

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

import fsclient.entities.OAuthVersion._

// FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
case class AccessTokenRequestV1(uri: Uri) extends FsAuthRequest.Post[Nothing, String, Version1.AccessToken] {
  final override val body: Option[Nothing] = None
}
