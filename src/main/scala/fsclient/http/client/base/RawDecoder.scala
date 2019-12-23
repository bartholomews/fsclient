package fsclient.http.client.base

import cats.effect.Effect
import fs2.Pipe

trait RawDecoder[Raw] {
  def decode[F[_]: Effect]: Pipe[F, Byte, Raw]
}
