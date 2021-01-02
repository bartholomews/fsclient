package io.bartholomews.fsclient.core.http

import sttp.client3.{asStringAlways, HttpError, MappedResponseAs, ResponseAs, ResponseException}

trait ResponseMapping[A, DE, B] {
  def raw: ResponseAs[A, Any]
  def errorToMessage: A => String
  def transform: A => Either[ResponseException[String, DE], B]

  final def responseAs: MappedResponseAs[A, Either[ResponseException[String, DE], B], Any] =
    MappedResponseAs[A, Either[ResponseException[String, DE], B], Any](
      raw,
      g = (body, meta) =>
        if (meta.isSuccess) transform(body)
        else Left(HttpError(errorToMessage(body), meta.code)),
      showAs = None
    )
}

object ResponseMapping {
  def plainTextTo[DE, T](f: String => Either[ResponseException[String, DE], T]): ResponseMapping[String, DE, T] =
    new ResponseMapping[String, DE, T] {
      final override val raw: ResponseAs[String, Any] = asStringAlways
      final override val errorToMessage: String => String = identity
      override def transform: String => Either[ResponseException[String, DE], T] = f
    }
}
