package io.bartholomews.fsclient.core.oauth.v2

import sttp.client.RequestT

// https://tools.ietf.org/html/rfc6749#section-2.2
case class ClientId(value: String) extends AnyVal
case class ClientSecret(value: String) extends AnyVal

// https://tools.ietf.org/html/rfc6749#section-2.3.1
case class ClientPassword(clientId: ClientId, clientSecret: ClientSecret) {
  def authorizationBasic[U[_], T](request: RequestT[U, T, Nothing]): RequestT[U, T, Nothing] =
    request.auth.basic(clientId.value, clientSecret.value)
  //    lazy val authorizationBasic: Header = FsHeaders.authorizationBasic(s"${clientId.value}:${clientSecret.value}")
}
