package io.bartholomews.fsclient.utils

import cats.effect.IO
import io.bartholomews.fsclient.entities.{ErrorBody, FsResponse}
import org.http4s.Status

object HttpTypes {
  type HttpResponse[A] = FsResponse[ErrorBody, A]
  type IOResponse[A] = IO[HttpResponse[A]]

  type ErrorOr[A] = Either[(Status, ErrorBody), A]
}
