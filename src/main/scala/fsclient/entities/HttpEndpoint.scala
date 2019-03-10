package fsclient.entities

import org.http4s.{Method, Uri}

trait HttpEndpoint[T] {
  def uri: Uri

  def method: Method
}
