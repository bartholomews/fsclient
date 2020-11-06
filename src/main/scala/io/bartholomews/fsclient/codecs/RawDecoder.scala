package io.bartholomews.fsclient.codecs

import cats.effect.Sync
import fs2.Pipe
import io.bartholomews.fsclient.utils.FsLogger.{rawJsonResponseLogPipe, rawPlainTextResponseLogPipe}
import io.circe.Json
import io.circe.fs2.byteStreamParser
import org.http4s.Response

trait RawDecoder[Raw] {
  def pipe[F[_]: Sync]: Pipe[F, Response[F], Raw]
}

object RawDecoder {
  val rawJsonPipe: RawDecoder[Json] = new RawDecoder[Json] {
    override def pipe[F[_]: Sync]: Pipe[F, Response[F], Json] = _.flatMap(
      _.body
        .through(byteStreamParser)
        .through(rawJsonResponseLogPipe)
    )
  }

  val rawPlainTextPipe: RawDecoder[String] = new RawDecoder[String] {
    override def pipe[F[_]: Sync]: Pipe[F, Response[F], String] = _.flatMap { response =>
      fs2.Stream
        .eval(response.as[String])
        .through(rawPlainTextResponseLogPipe)
    }
  }
}
