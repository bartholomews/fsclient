package fsclient.requests

import cats.effect.Effect
import fs2.Pipe
import io.circe.{Decoder, Json}

trait JsonEntityResponse[A] {
  implicit def deriveEntityJsonDecoder[F[_]: Effect](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    fsclient.implicits.deriveJsonPipe[F, A]
}
