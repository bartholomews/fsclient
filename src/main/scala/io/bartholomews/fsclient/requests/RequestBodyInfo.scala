package io.bartholomews.fsclient.requests

import io.circe.Encoder
import org.http4s.{EntityEncoder, UrlForm}
import org.http4s.circe.CirceEntityEncoder

private[fsclient] trait RequestBodyInfo[Body] {
  def body: Option[Body]
  def requestBodyEncoder[F[_]]: EntityEncoder[F, Body]
}

private[fsclient] trait EmptyRequestBody extends RequestBodyInfo[Nothing] {
  final override def body: Option[Nothing] = None
  final override def requestBodyEncoder[F[_]]: EntityEncoder[F, Nothing] =
    EntityEncoder.emptyEncoder[F, Nothing]
}

private[fsclient] trait HasRequestBodyAsJson[Body] extends RequestBodyInfo[Body] {
  def requestBody: Body
  def bodyEncoder: Encoder[Body]
  final override def body: Option[Body] = Some(requestBody)
  final override def requestBodyEncoder[F[_]]: EntityEncoder[F, Body] =
    CirceEntityEncoder.circeEntityEncoder(bodyEncoder)
}

private[fsclient] trait HasRequestBodyAsUrlForm extends RequestBodyInfo[UrlForm] {
  def requestBody: UrlForm
  final override def body: Option[UrlForm] = Some(requestBody)
  final override def requestBodyEncoder[F[_]]: EntityEncoder[F, UrlForm] =
    UrlForm.entityEncoder
}
