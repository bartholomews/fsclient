package io.bartholomews.fsclient.requests

import cats.effect.Effect
import io.bartholomews.fsclient.config.UserAgent
import io.bartholomews.fsclient.utils.FsHeaders
import io.bartholomews.fsclient.utils.FsLogger._
import org.http4s._

private[fsclient] trait FsClientRequest[Body] {
  def uri: Uri

  def headers: Headers = Headers.empty

  private[fsclient] def method: Method
  private[fsclient] def body: Option[Body]
  private[fsclient] def requestBodyEncoder[F[_]]: EntityEncoder[F, Body]

  final private[fsclient] def toHttpRequest[F[_]: Effect](
    userAgent: UserAgent
  ): Request[F] =
    logRequest {
      body
        .fold[Request[F]](Request())(b => Request().withEntity(b)(requestBodyEncoder))
        .withMethod(method)
        .withUri(uri)
        .withHeaders(headers.++(Headers.of(FsHeaders.userAgent(userAgent.value))))
    }(body)
}
