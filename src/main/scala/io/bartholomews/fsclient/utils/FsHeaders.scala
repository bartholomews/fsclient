package io.bartholomews.fsclient.utils

import java.nio.charset.StandardCharsets
import java.util.Base64

import io.bartholomews.fsclient.entities.oauth.v2.OAuthV2AuthorizationFramework
import org.apache.http.entity.ContentType
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, Header}

object FsHeaders {
  def accept(contentType: ContentType): Header = Header("accept", contentType.getMimeType)
  def contentType(contentType: ContentType): Header = Header("Content-Type", contentType.getMimeType)
  def userAgent(value: String): Header = Header("User-Agent", value)
  // https://tools.ietf.org/html/rfc7617
  def authorizationBasic(secret: String): Header = {
    val base64Secret = Base64.getEncoder.encodeToString(secret.getBytes(StandardCharsets.UTF_8))
    Header("Authorization", s"Basic $base64Secret")
  }
  // https://tools.ietf.org/html/rfc6750
  def authorizationBearer(accessToken: OAuthV2AuthorizationFramework.AccessToken): Header =
    Header("Authorization", s"Bearer ${accessToken.value}")

  def is(
    actualContentType: `Content-Type`,
    expectedContentType: ContentType,
    expectedCharset: Charset = Charset.`UTF-8`
  ): Boolean = {
    val `type` = s"${actualContentType.mediaType.mainType}/${actualContentType.mediaType.subType}"
    val unexpectedCharset = actualContentType.charset.exists(_ != expectedCharset)
    expectedContentType.getMimeType == `type` && !unexpectedCharset
  }
}
