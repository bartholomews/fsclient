package fsclient.requests

import cats.effect.Effect
import fs2.{text, Pipe}
import fsclient.http.client.base.RawDecoder
import fsclient.utils.Logger.{rawJsonResponseLogPipe, rawPlainTextResponseLogPipe}
import io.circe.fs2.byteStreamParser
import io.circe.{Decoder, Encoder, Json}
import org.http4s.EntityEncoder
import org.http4s.circe.jsonEncoderOf

trait CodecSyntax {

  implicit def deriveJsonBodyEncoder[F[_]: Effect, Body](implicit encode: Encoder[Body]): EntityEncoder[F, Body] =
    jsonEncoderOf[F, Body]

  implicit val rawPlainTextPipe: RawDecoder[String] = new RawDecoder[String] {
    override def decode[F[_]: Effect]: Pipe[F, Byte, String] =
      _.through(text.utf8Decode)
        .through(rawPlainTextResponseLogPipe)
  }

  implicit val rawJsonPipe: RawDecoder[Json] = new RawDecoder[Json] {
    override def decode[F[_]: Effect]: Pipe[F, Byte, Json] =
      _.through(byteStreamParser)
        .through(rawJsonResponseLogPipe)
  }

  implicit def stringDecoderPipe[F[_]: Effect]: Pipe[F, String, String] = _.map(identity)

  implicit def decodeJsonAsString[F[_]: Effect]: Pipe[F, Json, String] =
    deriveJsonPipe[F, String](implicitly, Decoder.decodeString)

  // diverging implicit expansion for type io.circe.Decoder[A] starting with lazy value decodeZoneOffset in object Decoder
  def deriveJsonPipe[F[_]: Effect, A](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    io.circe.fs2.decoder[F, A]
}
