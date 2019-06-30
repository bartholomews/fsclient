package fsclient.entities

import org.http4s.{Method, Uri}

trait HttpEndpoint[E] {
  def uri: Uri

  def method: Method
}
