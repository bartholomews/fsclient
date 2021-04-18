package io.bartholomews.scalatestudo

import io.bartholomews.fsclient.core.http.SttpResponses.ResponseHandler

trait ServerBehaviours[Encoder[_], Decoder[_], DE] {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[DE, T]
}
