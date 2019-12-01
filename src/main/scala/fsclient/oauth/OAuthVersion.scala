package fsclient.oauth

import cats.effect.Effect
import fsclient.defaultConfig
import fsclient.entities._
import io.circe.generic.extras.ConfiguredJsonCodec
import org.http4s.client.oauth1.{signRequest, Consumer, Token}
import org.http4s.{Method, Request, Uri}

sealed trait OAuthVersion

sealed trait OAuthToken

sealed trait OAuthTokenV1 extends OAuthToken {
  def token: Token
  def verifier: Option[String]
}

sealed trait OAuthTokenV2 extends OAuthToken

object OAuthVersion {
  // https://tools.ietf.org/html/rfc5849
  case object OAuthV1 extends OAuthVersion {

    case class RequestTokenV1 private (token: Token, verifier: Option[String]) extends OAuthTokenV1
    object RequestTokenV1 {
      def apply(token: Token, tokenVerifier: String) = new RequestTokenV1(token, Some(tokenVerifier))
    }

    case class AccessTokenV1 private (token: Token, verifier: Option[String]) extends OAuthTokenV1
    object AccessTokenV1 {
      def apply(token: Token) = new AccessTokenV1(token, verifier = None)
    }

    private[fsclient] def sign[F[_]: Effect](consumer: Consumer, v1: OAuthTokenV1)(req: Request[F]): F[Request[F]] =
      signRequest(
        req,
        consumer,
        callback = None,
        v1.verifier,
        Some(v1.token)
      )

    trait AccessTokenRequestV1 extends FsClientPlainRequest {
      def token: OAuthTokenV1
    }

    // FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
    object AccessTokenRequestV1 {
      def apply(requestUri: Uri, requestToken: RequestTokenV1): AccessTokenRequestV1 =
        new AccessTokenRequestV1 {
          override val token = RequestTokenV1(requestToken.token, requestToken.verifier)
          override val uri: Uri = requestUri
          override val method: Method = Method.POST
        }
    }
  }
  // https://tools.ietf.org/html/rfc6749
  case object OAuthV2 extends OAuthVersion {
    // https://tools.ietf.org/html/rfc6749#section-4.2.2
    @ConfiguredJsonCodec(decodeOnly = true)
    case class AccessTokenResponse(
      accessToken: String,
      tokenType: String,
      expiresIn: Option[Long],
      scope: Option[String],
      state: Option[String]
    )

    case class AccessTokenV2(value: String) extends OAuthTokenV2
  }
}
