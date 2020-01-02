package fsclient.codecs

import cats.effect.Effect
import fs2.Pipe
import fsclient.utils.Logger.{rawJsonResponseLogPipe, rawPlainTextResponseLogPipe}
import io.circe.fs2.byteStreamParser
import io.circe.{Decoder, Encoder, Json}
import org.http4s.circe.jsonEncoderOf
import org.http4s.{EntityEncoder, Response}

trait CodecSyntax extends PlainTextDecodingSyntax {

  implicit def emptyEntityEncoder[F[_]: Effect]: EntityEncoder[F, Nothing] = EntityEncoder.emptyEncoder[F, Nothing]

  implicit def deriveJsonBodyEncoder[F[_]: Effect, Body](implicit encode: Encoder[Body]): EntityEncoder[F, Body] =
    jsonEncoderOf[F, Body]

  implicit val rawPlainTextPipe: RawDecoder[String] = new RawDecoder[String] {
    override def decode[F[_]: Effect]: Pipe[F, Response[F], String] =
      _.flatMap(response => {
        fs2.Stream
          .eval(response.as[String])
          .through(rawPlainTextResponseLogPipe)
      })
  }

  implicit val rawJsonPipe: RawDecoder[Json] = new RawDecoder[Json] {
    override def decode[F[_]: Effect]: Pipe[F, Response[F], Json] =
      _.flatMap(
        _.body
          .through(byteStreamParser)
          .through(rawJsonResponseLogPipe)
      )
  }

  // implicit def decodeIdentity[F[_]: Effect, A]: Pipe[F, A, A] = _.map(identity)
  implicit def stringDecoderPipe[F[_]: Effect]: Pipe[F, String, String] = _.map(identity)

  implicit def decodeJsonAsString[F[_]: Effect]: Pipe[F, Json, String] =
    deriveJsonPipe[F, String](implicitly[Effect[F]], Decoder.decodeString)

  // diverging implicit expansion for type io.circe.Decoder[A] starting with lazy value decodeZoneOffset in object Decoder
  def deriveJsonPipe[F[_]: Effect, A](implicit decode: Decoder[A]): Pipe[F, Json, A] = io.circe.fs2.decoder[F, A]
}
