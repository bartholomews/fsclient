package io.bartholomews.fsclient.requests

import cats.effect.ConcurrentEffect
import fs2.Pipe
import io.bartholomews.fsclient.codecs.{RawDecoder, StringPipe}
import io.circe.{Decoder, Json}

// Knows how to decode a `Raw` Content-Type into a `Res`
trait HasDecoder[Raw, Res] {
  def rawDecoder: RawDecoder[Raw]
  def pipeDecoder[F[_]: ConcurrentEffect]: Pipe[F, Raw, Res]
}

trait HasJsonDecoder[Res] extends HasDecoder[Json, Res] {
  def resDecoder: Decoder[Res]
  final override def rawDecoder: RawDecoder[Json] = RawDecoder.rawJsonPipe
  final override def pipeDecoder[F[_]: ConcurrentEffect]: Pipe[F, Json, Res] =
    io.circe.fs2.decoder[F, Res](implicitly[ConcurrentEffect[F]], resDecoder)
}

trait HasPlainTextDecoder[Res] extends HasDecoder[String, Res] {
  def resDecoder: StringPipe[Res]
  final override def rawDecoder: RawDecoder[String] = RawDecoder.rawPlainTextPipe
  final override def pipeDecoder[F[_]: ConcurrentEffect]: Pipe[F, String, Res] =
    resDecoder.decodePipe
}
