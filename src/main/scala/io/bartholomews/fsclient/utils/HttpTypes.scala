package io.bartholomews.fsclient.utils

import cats.effect.IO
import io.bartholomews.fsclient.entities.{FsResponse, HttpError, OAuthVersion}

object HttpTypes {
  type HttpResponse[V <: OAuthVersion, A] = FsResponse[V, HttpError, A]
  type IOResponse[V <: OAuthVersion, A] = IO[HttpResponse[V, A]]
  type ErrorOr[A] = Either[HttpError, A]
}
