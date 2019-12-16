package fsclient.utils

import cats.effect.IO
import fsclient.requests.{HttpResponse, ResponseError}
import fs2.Pipe

object HttpTypes {
  type HttpPipe[F[_], A, B] = Pipe[F, ErrorOr[A], ErrorOr[B]]
  type IOHttpPipe[A, B] = HttpPipe[IO, A, B]
  type IOResponse[T] = IO[HttpResponse[T]]
  type ErrorOr[T] = Either[ResponseError, T]
}
