package io.bartholomews.fsclient.requests

import cats.effect.Effect
import io.bartholomews.fsclient.config.UserAgent
import io.bartholomews.fsclient.utils.FsHeaders
import io.bartholomews.fsclient.utils.Logger._
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
