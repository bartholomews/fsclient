package io.bartholomews.fsclient.codecs

import cats.implicits._
import fs2.{INothing, Pipe, RaiseThrowable}
import io.bartholomews.fsclient.entities.EmptyResponseException

trait PlainTextDecodingSyntax {

  private def raiseError[F[_]: RaiseThrowable](either: Either[String, String]): fs2.Stream[F, INothing] =
    fs2.Stream.raiseError[F] {
      either match {
        case Right(response) => new Exception(s"Unexpected response: $response")
        case Left(error)     => if (error.isEmpty) EmptyResponseException() else new Exception(error)
      }
    }

  final def plainTextDecoderPipe[F[_]: RaiseThrowable, A](
    pf: PartialFunction[Either[String, String], A]
  ): Pipe[F, String, A] =
    _.attempt
      .map(_.leftMap(_.getMessage))
      .flatMap(pf.andThen(entity => fs2.Stream.emit(entity)).orElse { case other =>
        raiseError[F](other)
      })
}
