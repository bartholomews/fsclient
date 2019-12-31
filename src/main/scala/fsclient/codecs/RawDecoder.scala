package fsclient.codecs

import cats.effect.Effect
import fs2.Pipe
import org.http4s.Response

trait RawDecoder[Raw] {
  def decode[F[_]: Effect]: Pipe[F, Response[F], Raw]
}
