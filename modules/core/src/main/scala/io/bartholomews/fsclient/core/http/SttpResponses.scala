package io.bartholomews.fsclient.core.http

import sttp.client.{Response, ResponseError}

object SttpResponses {
  type SttpResponse[DE, T] = Response[Either[ResponseError[DE], T]]
}
