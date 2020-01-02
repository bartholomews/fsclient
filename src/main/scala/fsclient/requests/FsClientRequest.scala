package fsclient.requests

import cats.effect.Effect
import fsclient.config.AppConsumer
import fsclient.utils.FsHeaders
import fsclient.utils.Logger._
import org.http4s._

// FIXME: Find a good way to unify these combinations: `Simple/Auth`
private[fsclient] trait FsClientRequest[Body] {
  def uri: Uri

  def method: Method

  def headers: Headers = Headers.empty

  def body: Option[Body] = None

  final private[fsclient] def toHttpRequest[F[_]: Effect](
    consumer: AppConsumer
  )(implicit requestBodyEncoder: EntityEncoder[F, Body]): Request[F] =
    logRequest(
      body,
      body.fold[Request[F]](Request())(b => Request().withEntity(b))
    ).withMethod(method)
      .withUri(uri)
      .withHeaders(headers.++(Headers.of(FsHeaders.userAgent(consumer.userAgent))))
}
