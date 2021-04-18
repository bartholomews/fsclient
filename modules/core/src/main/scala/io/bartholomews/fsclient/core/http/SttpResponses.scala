package io.bartholomews.fsclient.core.http

import sttp.client3.{Response, ResponseAs, ResponseException}

object SttpResponses {
  type SttpResponse[DE, T] = Response[Either[ResponseException[String, DE], T]]
  type ResponseHandler[+E, T] = ResponseAs[Either[ResponseException[String, E], T], Any]
}
