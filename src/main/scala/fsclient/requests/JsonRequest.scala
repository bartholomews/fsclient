package fsclient.requests

import cats.effect.Effect
import io.circe.Encoder
import org.http4s.EntityEncoder

trait JsonRequest {
  import org.http4s.circe._
  implicit def entityJsonEncoder[F[_]: Effect, Body](implicit encoder: Encoder[Body]): EntityEncoder[F, Body] =
    jsonEncoderOf[F, Body]
}
