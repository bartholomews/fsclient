package fsclient.oauth

import java.nio.charset.StandardCharsets
import java.util.Base64

import org.apache.http.entity.ContentType
import org.http4s.Header

object FsHeaders {
  def accept(contentType: ContentType): Header = Header("accept", contentType.getMimeType)
  // https://tools.ietf.org/html/rfc7617
  def basicAuthentication(secret: String): Header = {
    val base64Secret = Base64.getEncoder.encodeToString(secret.getBytes(StandardCharsets.UTF_8))
    Header("Authorization", s"Basic $base64Secret")
  }
}
