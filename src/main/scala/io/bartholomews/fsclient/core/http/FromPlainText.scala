package io.bartholomews.fsclient.core.http

import sttp.client.DeserializationError

trait FromPlainText[T] {
  def decode(str: String): Either[DeserializationError[Exception], T]
}
