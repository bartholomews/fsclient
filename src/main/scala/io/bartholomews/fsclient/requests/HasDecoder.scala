package io.bartholomews.fsclient.requests

import io.bartholomews.fsclient.codecs.{RawDecoder, ResDecoder}
import io.circe.Json

// Knows how to decode a `Raw` Content-Type into a `Res`
trait HasDecoder[Raw, Res] {
  def rawDecoder: RawDecoder[Raw]
  def resDecoder: ResDecoder[Raw, Res]
}

trait HasJsonDecoder[Res] extends HasDecoder[Json, Res] {
  def resDecoder: ResDecoder[Json, Res]
  final override def rawDecoder: RawDecoder[Json] = RawDecoder.rawJsonPipe
}

trait HasPlainTextDecoder[Res] extends HasDecoder[String, Res] {
  def resDecoder: ResDecoder[String, Res]
  final override def rawDecoder: RawDecoder[String] = RawDecoder.rawPlainTextPipe
}
