package fsclient.requests

import fsclient.entities.OAuthVersion.Version2
import fsclient.requests.ClientCredentials.{ClientId, ClientSecret}
import fsclient.utils.FsHeaders
import org.apache.http.entity.ContentType
import org.http4s.{Header, Headers}

// https://tools.ietf.org/html/rfc6749#section-4.4.2
trait AccessTokenRequestClientCredentials extends PlainTextRequest.Post[String, Version2.AccessTokenResponse] {
  def clientId: ClientId
  def clientSecret: ClientSecret

  final override val entityBody = "grant_type=client_credentials"

  final override val headers: Headers = Headers(
    List(
      Header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType),
      FsHeaders.authorizationBasic(s"${clientId.value}:${clientSecret.value}")
    )
  )
}

object ClientCredentials {
  case class ClientId(value: String) extends AnyVal
  case class ClientSecret(value: String) extends AnyVal
}
