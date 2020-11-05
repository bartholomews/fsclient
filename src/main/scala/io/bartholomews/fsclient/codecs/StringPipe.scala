package io.bartholomews.fsclient.codecs

import cats.implicits.catsSyntaxEitherId
import fs2.{Pipe, RaiseThrowable}

trait StringPipe[Res] {
  def decode(str: String): Either[Throwable, Res]
  final def decodePipe[F[_]: RaiseThrowable]: Pipe[F, String, Res] =
    _.flatMap(str => decode(str).fold(fs2.Stream.raiseError[F], fs2.Stream.emit))
}

object StringPipe {
  implicit val stringPipeIdentity: StringPipe[String] = _.asRight
}
