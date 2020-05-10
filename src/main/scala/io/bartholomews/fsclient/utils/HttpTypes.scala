package io.bartholomews.fsclient.utils

import cats.effect.IO
import io.bartholomews.fsclient.entities.{ErrorBody, FsResponse, HttpError}

object HttpTypes {
  type HttpResponse[A] = FsResponse[ErrorBody, A]
  type IOResponse[A] = IO[HttpResponse[A]]
  type ErrorOr[A] = Either[HttpError, A]
}
