package io.bartholomews.fsclient.codecs

import cats.implicits.catsSyntaxEitherId
import fs2.{Pipe, RaiseThrowable}
import io.circe.{Decoder, Json}

trait ResDecoder[Raw, Res] {
  def decode(raw: Raw): Either[Throwable, Res]
  final def pipe[F[_]: RaiseThrowable]: Pipe[F, Raw, Res] =
    _.flatMap(str => decode(str).fold(fs2.Stream.raiseError[F], fs2.Stream.emit))
}

object ResDecoder {
  implicit val stringPipeIdentity: ResDecoder[String, String] = _.asRight
  implicit def unitResDecoder[Raw]: ResDecoder[Raw, Unit] = _ => Right(())
  implicit def jsonResDecoder[Res](implicit decoder: Decoder[Res]): ResDecoder[Json, Res] =
    (raw: Json) => decoder.apply(raw.hcursor)
}
