package fsclient.utils

import cats.effect.IO
import fsclient.entities.{FsResponse, HttpError}

object HttpTypes {
  type HttpResponse[A] = FsResponse[HttpError, A]
  type IOResponse[A] = IO[HttpResponse[A]]
  type ErrorOr[A] = Either[HttpError, A]
}
