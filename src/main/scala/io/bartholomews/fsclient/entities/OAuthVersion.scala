package io.bartholomews.fsclient.entities

import cats.effect.Effect
import io.bartholomews.fsclient.codecs.FsJsonResponsePipe
import io.bartholomews.fsclient.entities.OAuthVersion.Version2.AccessTokenV2
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.{AccessToken, ClientPassword, RefreshToken}
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.client.oauth1.{signRequest, Consumer, Token}
import org.http4s.{Request, Uri}

sealed trait OAuthVersion

sealed trait Signer[+O <: OAuthVersion]

sealed trait SignerV1 extends Signer[OAuthVersion.V1] {
  def consumer: Consumer
}

case class SignerV2(tokenEndpoint: Uri, clientPassword: ClientPassword, accessTokenResponse: AccessTokenV2)
    extends Signer[OAuthVersion.V2]

object OAuthVersion {

  type V1 = OAuthVersion.Version1.type
  type V2 = OAuthVersion.Version2.type

  // https://tools.ietf.org/html/rfc5849 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  case object Version1 extends OAuthVersion {

    // Sign with consumer key/secret, but without token (i.e. not a full OAuth request)
    case class BasicSignature(consumer: Consumer) extends SignerV1

    // Full OAuth v1 request
    sealed trait TokenV1 extends SignerV1 {
      def token: Token

      private[fsclient] def tokenVerifier: Option[String]
    }

    // https://tools.ietf.org/html/rfc5849#section-2.2
    case class RequestTokenV1(token: Token, verifier: String, consumer: Consumer) extends TokenV1 {
      final override private[fsclient] val tokenVerifier: Option[String] = Some(verifier)
    }

    // https://tools.ietf.org/html/rfc5849#section-2.3
    case class AccessTokenV1 private (token: Token, tokenVerifier: Option[String], consumer: Consumer) extends TokenV1
    object AccessTokenV1 {
      def apply(token: Token, consumer: Consumer) = new AccessTokenV1(token, tokenVerifier = None, consumer)
    }

    private[fsclient] def sign[F[_]: Effect](v1: BasicSignature)(req: Request[F]): F[Request[F]] =
      signRequest(req, v1.consumer, callback = None, verifier = None, token = None)

    private[fsclient] def sign[F[_]: Effect](v1: TokenV1)(req: Request[F]): F[Request[F]] =
      signRequest(req, v1.consumer, callback = None, v1.tokenVerifier, Some(v1.token))
  }

  // https://tools.ietf.org/html/rfc6749 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  case object Version2 extends OAuthVersion {

    // https://tools.ietf.org/html/rfc6749#section-5.1
    case class AccessTokenV2(
      accessToken: AccessToken,
      // https://tools.ietf.org/html/rfc6749#section-7.1
      tokenType: String,
      expiresIn: Option[Long], // RECOMMENDED.  The lifetime in seconds of the access token.
      refreshToken: Option[RefreshToken],
      // https://tools.ietf.org/html/rfc6749#section-3.3
      scope: Option[String]
    ) {
      private val generatedAt: Long = System.currentTimeMillis()

      def isExpired: Option[Boolean] =
        expiresIn.map(expInSec => System.currentTimeMillis() > generatedAt + (expInSec * 1000))
    }

    object AccessTokenV2 extends FsJsonResponsePipe[AccessTokenV2] {
      implicit val decoder: Decoder[AccessTokenV2] = deriveConfiguredDecoder
    }

    // https://tools.ietf.org/html/rfc6749#section-4.2.2
    case object ImplicitGrantAccessTokenResponse
  }

}
