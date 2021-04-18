package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.core.http.SttpResponses.ResponseHandler
import io.circe.Decoder

trait CirceServerBehaviours {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[io.circe.Error, T] =
    codecs.responseHandler
}
