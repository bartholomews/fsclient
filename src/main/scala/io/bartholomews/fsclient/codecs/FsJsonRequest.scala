package io.bartholomews.fsclient.codecs

import cats.effect.ConcurrentEffect
import io.circe.Encoder
import org.http4s.EntityEncoder

trait FsJsonRequest[A] {
  implicit def deriveEntityJsonEncoder[F[_]: ConcurrentEffect](implicit encoder: Encoder[A]): EntityEncoder[F, A] =
    io.bartholomews.fsclient.implicits.deriveJsonBodyEncoder[F, A]
}
