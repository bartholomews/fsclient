package io.bartholomews.fsclient.core.http

import sttp.client.{asStringAlways, HttpError, MappedResponseAs, ResponseAs, ResponseError}

trait ResponseMapping[A, E, B] {
  def raw: ResponseAs[A, Nothing]
  def errorToMessage: A => String
  def transform: A => Either[ResponseError[E], B]

  final def responseAs: MappedResponseAs[A, Either[ResponseError[E], B], Nothing] =
    MappedResponseAs[A, Either[ResponseError[E], B], Nothing](
      raw,
      g = (body, meta) =>
        if (meta.isSuccess) transform(body)
        else Left(HttpError(errorToMessage(body), meta.code))
    )
}

object ResponseMapping {
  def plainTextTo[E, T](f: String => Either[ResponseError[E], T]): ResponseMapping[String, E, T] =
    new ResponseMapping[String, E, T] {
      final override val raw: ResponseAs[String, Nothing] = asStringAlways
      final override val errorToMessage: String => String = identity
      override def transform: String => Either[ResponseError[E], T] = f
    }
}
