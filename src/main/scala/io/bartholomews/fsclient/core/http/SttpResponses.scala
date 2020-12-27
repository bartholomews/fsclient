package io.bartholomews.fsclient.core.http

import sttp.client.{Response, ResponseError}

object SttpResponses {
  type SttpResponse[E <: ResponseError[_], T] = Response[Either[E, T]]
  // FIXME: circe should be in its own module not in core
  type CirceJsonResponse[T] = SttpResponse[ResponseError[io.circe.Error], T]
}
