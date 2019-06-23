package fsclient.entities

import org.http4s.Uri

trait HttpEndpoint[E, M <: HttpMethod] {
  def uri: Uri

  def method: M
}
