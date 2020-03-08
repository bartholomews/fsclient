package fsclient.codecs

import cats.effect.Effect
import cats.implicits._
import fs2.{INothing, Pipe}
import fsclient.entities.EmptyResponseException

trait PlainTextDecodingSyntax {

  private def raiseError[F[_]: Effect](either: Either[String, String]): fs2.Stream[F, INothing] =
    fs2.Stream.raiseError[F] {
      either match {
        case Right(response) => new Exception(s"Unexpected response: $response")
        case Left(error)     => if (error.isEmpty) EmptyResponseException else new Exception(error)
      }
    }

  final def plainTextDecoderPipe[F[_]: Effect, A](
    pf: PartialFunction[Either[String, String], A]
  ): Pipe[F, String, A] =
    _.attempt
      .map(_.leftMap(_.getMessage))
      .flatMap(pf.andThen(entity => fs2.Stream.emit(entity)).orElse {
        case other => raiseError[F](other)
      })
}
