package fsclient.utils

import cats.effect.IO
import fsclient.entities.{FsResponse, HttpError}

object HttpTypes {
  type IOResponse[A] = IO[FsResponse[HttpError, A]]
  type ErrorOr[A] = Either[HttpError, A]
}
