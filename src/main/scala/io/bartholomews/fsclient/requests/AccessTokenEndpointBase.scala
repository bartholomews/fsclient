package io.bartholomews.fsclient.requests

import org.http4s.Uri

trait AccessTokenEndpointBase {
  def uri: Uri
}
