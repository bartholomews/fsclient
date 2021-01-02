package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.circe.oauth.CirceOAuthCodecs
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ResponseHandler
import io.circe
import io.circe.{Codec, Decoder, Encoder}
import sttp.client3.circe.SttpCirceApi
import sttp.model.Uri

trait FsClientCirceApi extends SttpCirceApi with CirceOAuthCodecs {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[circe.Error, T] =
    asJson[T]

  def dropNullValues[A](encoder: Encoder[A]): Encoder[A] = encoder.mapJson(_.dropNullValues)

  implicit val sttpUriCodec: Codec[Uri] = Codec.from(
    Decoder.decodeString.emap(Uri.parse),
    Encoder.encodeString.contramap(_.toString())
  )
}
