package io.bartholomews.fsclient.utils

import cats.effect.IO
import io.bartholomews.fsclient.entities.{FsResponse, HttpError}

object HttpTypes {
  type HttpResponse[A] = FsResponse[HttpError, A]
  type IOResponse[A] = IO[HttpResponse[A]]
  type ErrorOr[A] = Either[HttpError, A]
}
