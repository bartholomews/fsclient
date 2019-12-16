package fsclient.http.client.base

sealed trait ResponseType

object ResponseType {
  case object Json extends ResponseType
  case object PlainText extends ResponseType
}
