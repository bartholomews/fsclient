package fsclient.codecs

import cats.effect.Effect
import fs2.Pipe
import io.circe.{Decoder, Json}

// FIXME: You could try with a macro annotation like @ConfiguredFsJsonResponsePipe
trait FsJsonResponsePipe[A] {
  implicit def deriveEntityJsonDecoder[F[_]: Effect](implicit decode: Decoder[A]): Pipe[F, Json, A] =
    fsclient.implicits.deriveJsonPipe[F, A]
}
