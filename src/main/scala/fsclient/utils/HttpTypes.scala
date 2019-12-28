package fsclient.utils

import cats.effect.IO
import fsclient.entities.{HttpResponse, ResponseError}

object HttpTypes {
  type IOResponse[T] = IO[HttpResponse[T]]
  type ErrorOr[T] = Either[ResponseError, T]
}
