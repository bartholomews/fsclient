package io.bartholomews.scalatestudo

import io.bartholomews.fsclient.core.http.SttpResponses.ResponseHandler
import io.bartholomews.scalatestudo.entities.JsonCodecs
import sttp.client3.BodySerializer

trait ServerBehaviours[Encoder[_], Decoder[_], DE, Json] {
  implicit def bodySerializer[T](implicit encoder: Encoder[T]): BodySerializer[T]
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[DE, T]

  def entityCodecs[Entity](implicit
    encoder: Encoder[Entity],
    decoder: Decoder[Entity]
  ): JsonCodecs[Entity, Encoder, Decoder, Json]
}
