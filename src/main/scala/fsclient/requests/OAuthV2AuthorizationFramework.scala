package fsclient.requests

import cats.data.Chain
import fsclient.entities.OAuthVersion.Version2
import fsclient.utils.FsHeaders
import org.http4s.{Header, Headers, Uri, UrlForm}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//
//  The OAuth 2.0 Authorization Framework
//
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// https://tools.ietf.org/html/rfc6749
object OAuthV2AuthorizationFramework {
  // https://tools.ietf.org/html/rfc6749#section-2.3.1
  case class ClientPassword(clientId: ClientId, clientSecret: ClientSecret) {
    lazy val authorizationBasic: Header = FsHeaders.authorizationBasic(s"${clientId.value}:${clientSecret.value}")
  }
  case class ClientId(value: String) extends AnyVal
  case class ClientSecret(value: String) extends AnyVal
  // https://tools.ietf.org/html/rfc6749#section-3.1.2
  case class RedirectUri(value: Uri)

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  //  Authorization Code Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.1
  object AuthorizationCodeGrant {

    // https://tools.ietf.org/html/rfc6749#section-4.1.1
    def authorizeUri(clientId: ClientId, redirectUri: Uri, state: Option[String], scopes: List[String])(
      serverUri: Uri
    ): Uri =
      serverUri
        .withQueryParam("client_id", clientId.value)
        .withQueryParam("response_type", "code")
        .withQueryParam("redirect_uri", redirectUri.renderString)
        .withOptionQueryParam("state", state)
        .withOptionQueryParam("scopes", if (scopes.isEmpty) None else Some(scopes.mkString(" ")))

    // https://tools.ietf.org/html/rfc6749#section-4.1.3
    trait AccessTokenRequest extends JsonRequest.Post[UrlForm, Version2.AccessTokenResponse] {
      def code: String
      def clientPassword: ClientPassword
      def redirectUri: Option[RedirectUri]
      override val headers: Headers = Headers.of(clientPassword.authorizationBasic)
      final override val entityBody = UrlForm(
        Map(
          "grant_type" -> Chain("authorization_code"),
          "code" -> Chain(code),
          "redirect_uri" -> redirectUri.fold(Chain.empty[String])(uri => Chain.one(uri.value.renderString))
        )
      )
    }
  }

  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // Client Credentials Grant
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  // https://tools.ietf.org/html/rfc6749#section-4.4
  object ClientCredentialsGrant {
    // https://tools.ietf.org/html/rfc6749#section-4.4.2
    trait AccessTokenRequest extends JsonRequest.Post[UrlForm, Version2.AccessTokenResponse] {
      def clientPassword: ClientPassword
      final override val entityBody = UrlForm(("grant_type", "client_credentials"))
      // TODO: Test that the content-type urlencoded working is added automatically by `entityEncoder[UrlForm]`
      override val headers: Headers = Headers.of(clientPassword.authorizationBasic)
    }
  }
  // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
