package io.bartholomews.fsclient.codecs

import cats.effect.{ConcurrentEffect, Effect, Sync}
import fs2.Pipe
import io.bartholomews.fsclient.utils.FsLogger.{rawJsonResponseLogPipe, rawPlainTextResponseLogPipe}
import io.circe.fs2.byteStreamParser
import io.circe.{Decoder, Encoder, Json}
import org.http4s.circe.jsonEncoderOf
import org.http4s.{EntityEncoder, Response, UrlForm}

trait CodecSyntax extends PlainTextDecodingSyntax {

  implicit def emptyEntityEncoder[F[_]]: EntityEncoder[F, Nothing] = EntityEncoder.emptyEncoder[F, Nothing]

  implicit def deriveJsonBodyEncoder[F[_]: ConcurrentEffect, Body](implicit encode: Encoder[Body]): EntityEncoder[F, Body] =
    jsonEncoderOf[F, Body]

  implicit def urlFormEntityEncoder[F[_]]: EntityEncoder[F, UrlForm] = UrlForm.entityEncoder

  implicit val rawPlainTextPipe: RawDecoder[String] = new RawDecoder[String] {
    override def decode[F[_]: Effect]: Pipe[F, Response[F], String] =
      _.flatMap { response =>
        fs2.Stream
          .eval(response.as[String])
          .through(rawPlainTextResponseLogPipe)
      }
  }

  implicit val rawJsonPipe: RawDecoder[Json] = new RawDecoder[Json] {
    override def decode[F[_]: Effect]: Pipe[F, Response[F], Json] =
      _.flatMap(
        _.body
          .through(byteStreamParser)
          .through(rawJsonResponseLogPipe)
      )
  }

  // implicit def decodeIdentity[F[_], A]: Pipe[F, A, A] = _.map(identity)
  implicit def stringDecoderPipe[F[_]]: Pipe[F, String, String] = _.map(identity)

  implicit def decodeJsonAsString[F[_]: Sync]: Pipe[F, Json, String] =
    deriveJsonPipe[F, String](implicitly[Sync[F]], Decoder.decodeString)

  implicit def decodeJsonAsInt[F[_]: Sync]: Pipe[F, Json, Int] =
    deriveJsonPipe[F, Int](implicitly[Sync[F]], Decoder.decodeInt)

  // diverging implicit expansion for type io.circe.Decoder[A] starting with lazy value decodeZoneOffset in object Decoder
  def deriveJsonPipe[F[_]: Sync, A](implicit decode: Decoder[A]): Pipe[F, Json, A] = io.circe.fs2.decoder[F, A]
}
