package fsclient.requests

import cats.effect.Effect
import fsclient.config.AppConsumer
import fsclient.oauth.FsHeaders
import fsclient.utils.Logger._
import org.http4s._

// FIXME: Find a good way to unify these combinations: `Simple/Auth` / `NoBody/WithBody` / `Json/PlainText`

trait FsClientPlainRequest {
  def uri: Uri
  def method: Method
  def headers: Headers = Headers.empty
  final private[fsclient] def toHttpRequest[F[_]: Effect](consumer: AppConsumer): Request[F] =
    logRequest {
      Request()
        .withMethod(method)
        .withUri(uri)
        .withHeaders(headers.++(Headers.of(FsHeaders.userAgent(consumer.userAgent))))
    }
}

trait FsClientRequestWithBody[Body] {
  def uri: Uri
  def method: Method
  def headers: Headers = Headers.empty
  def body: Body
  final private[fsclient] def toHttpRequest[F[_]: Effect](
    consumer: AppConsumer
  )(implicit requestBodyEncoder: EntityEncoder[F, Body]): Request[F] =
    logRequest(
      body,
      Request()
        .withMethod(method)
        .withUri(uri)
        .withEntity(body)
        .withHeaders(headers.++(Headers.of(FsHeaders.userAgent(consumer.userAgent))))
    )
}