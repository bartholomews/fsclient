package io.bartholomews.fsclient.core.oauth.v2

import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.RedirectUri
import sttp.model.Uri

sealed trait AuthorizationRequest {
  def responseType: String
  def clientId: ClientId
  def redirectUri: RedirectUri
  def state: Option[String]
  def scopes: List[String]

  final def uri(serverUri: Uri): Uri = {
    val queryParams: List[(String, String)] = {
      val requiredQueryParams: List[(String, String)] = List(
        ("client_id", clientId.value),
        ("response_type", responseType),
        ("redirect_uri", redirectUri.value.toString)
      )

      val optionalQueryParams = List(
        state.map(value => ("state", value)),
        if (scopes.isEmpty) None else Some(scopes.mkString(" ")).map(value => ("scope", value))
      ).flatten

      requiredQueryParams ++ optionalQueryParams
    }

    serverUri.addParams(queryParams: _*)
  }
}

/*
  Authorization Code Grant
  https://tools.ietf.org/html/rfc6749#section-4.1.1
 */
final case class AuthorizationCodeRequest(
  clientId: ClientId,
  redirectUri: RedirectUri,
  state: Option[String],
  scopes: List[String]
) extends AuthorizationRequest {

  override val responseType: String = "code"
}

/*
  Implicit Grant
  https://tools.ietf.org/html/rfc6749#section-4.2.1
 */
final case class AuthorizationTokenRequest(
  clientId: ClientId,
  redirectUri: RedirectUri,
  state: Option[String],
  scopes: List[String]
) extends AuthorizationRequest {

  override val responseType: String = "token"
}
