package io.bartholomews.fsclient.core.http

import sttp.client.{asStringAlways, HttpError, MappedResponseAs, ResponseAs, ResponseError}

trait ResponseMapping[A, B] {
  def raw: ResponseAs[A, Nothing]
  def errorToMessage: A => String
  def transform: A => Either[ResponseError[Exception], B]

  final def responseAs: MappedResponseAs[A, Either[ResponseError[Exception], B], Nothing] =
    MappedResponseAs[A, Either[ResponseError[Exception], B], Nothing](
      raw,
      g = (body, meta) =>
        if (meta.isSuccess) transform(body)
        else Left(HttpError(errorToMessage(body), meta.code))
    )
}

object ResponseMapping {
  def plainTextTo[T](f: String => Either[ResponseError[Exception], T]): ResponseMapping[String, T] =
    new ResponseMapping[String, T] {
      final override val raw: ResponseAs[String, Nothing] = asStringAlways
      final override val errorToMessage: String => String = identity
      override def transform: String => Either[ResponseError[Exception], T] = f
    }
}
