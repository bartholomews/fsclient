package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.ServerBehaviours
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ResponseHandler
import io.circe
import io.circe.Decoder

trait CirceServerBehaviours extends ServerBehaviours[circe.Encoder, circe.Decoder, circe.Error] {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[io.circe.Error, T] =
    codecs.responseHandler
}
