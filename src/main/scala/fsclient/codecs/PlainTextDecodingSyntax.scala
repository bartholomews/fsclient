package fsclient.codecs

import cats.effect.Effect
import fs2.{INothing, Pipe}
import fsclient.entities.EmptyResponseException
import cats.implicits._

import scala.util.matching.Regex

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

  final def plainTextRegexDecoderPipe1[F[_]: Effect, A](reg: Regex)(fn: String => A): Pipe[F, String, A] =
    plainTextDecoderPipe({
      case Right(reg(a)) => fn(a)
    })

  final def plainTextRegexDecoderPipe2[F[_]: Effect, A](reg: Regex)(fn: (String, String) => A): Pipe[F, String, A] =
    plainTextDecoderPipe({
      case Right(reg(a, b)) => fn(a, b)
    })

  final def plainTextRegexDecoderPipe3[F[_]: Effect, A](
    reg: Regex
  )(fn: (String, String, String) => A): Pipe[F, String, A] =
    plainTextDecoderPipe({
      case Right(reg(a, b, c)) => fn(a, b, c)
    })

  final def plainTextRegexDecoderPipe4[F[_]: Effect, A](
    reg: Regex
  )(fn: (String, String, String, String) => A): Pipe[F, String, A] =
    plainTextDecoderPipe({
      case Right(reg(a, b, c, d)) => fn(a, b, c, d)
    })

  final def plainTextRegexDecoderPipe5[F[_]: Effect, A](
    reg: Regex
  )(fn: (String, String, String, String, String) => A): Pipe[F, String, A] =
    plainTextDecoderPipe({
      case Right(reg(a, b, c, d, e)) => fn(a, b, c, d, e)
    })

  final def plainTextRegexDecoderPipe6[F[_]: Effect, A](
    reg: Regex
  )(fn: (String, String, String, String, String, String) => A): Pipe[F, String, A] =
    plainTextDecoderPipe({
      case Right(reg(a, b, c, d, e, f)) => fn(a, b, c, d, e, f)
    })
}
