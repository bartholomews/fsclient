package io.bartholomews.fsclient.core.http

import sttp.client3.{Response, ResponseException}

object SttpResponses {
  type SttpResponse[DE, T] = Response[Either[ResponseException[String, DE], T]]
}
