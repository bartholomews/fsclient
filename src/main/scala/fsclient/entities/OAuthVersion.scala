package fsclient.entities

import cats.effect.Effect
import fsclient.codecs.FsJsonResponsePipe
import fsclient.defaultConfig
import io.circe.Decoder
import io.circe.generic.extras._
import org.http4s.Request
import org.http4s.client.oauth1.{signRequest, Consumer, Token}

sealed trait OAuthVersion

sealed trait Signer[+O <: OAuthVersion] {
  def consumer: Consumer
}

object OAuthVersion {
  // https://tools.ietf.org/html/rfc5849 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  sealed case object V1 extends OAuthVersion {

    // Sign with consumer key/secret, but without token (i.e. not a full OAuth request)
    case class BasicSignature(consumer: Consumer) extends Signer[OAuthVersion.V1.type]

    // Full OAuth v1 request
    sealed trait SignerV1 extends Signer[OAuthVersion.V1.type] {
      def token: Token
      private[fsclient] def tokenVerifier: Option[String]
    }

    case class RequestToken(token: Token, verifier: String, consumer: Consumer) extends SignerV1 {
      final override private[fsclient] val tokenVerifier: Option[String] = Some(verifier)
    }

    case class AccessToken private (token: Token, tokenVerifier: Option[String], consumer: Consumer) extends SignerV1
    object AccessToken {
      def apply(token: Token, consumer: Consumer) = new AccessToken(token, tokenVerifier = None, consumer)
    }

    private[fsclient] def sign[F[_]: Effect](v1: BasicSignature)(req: Request[F]): F[Request[F]] =
      signRequest(req, v1.consumer, callback = None, verifier = None, token = None)

    private[fsclient] def sign[F[_]: Effect](v1: SignerV1)(req: Request[F]): F[Request[F]] =
      signRequest(req, v1.consumer, callback = None, v1.tokenVerifier, Some(v1.token))
  }
  // https://tools.ietf.org/html/rfc6749 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  sealed case object V2 extends OAuthVersion {
    // https://tools.ietf.org/html/rfc6749#section-4.2.2
    case class AccessTokenResponse(
      accessToken: String,
      tokenType: String,
      expiresIn: Option[Long],
      scope: Option[String],
      state: Option[String]
    )

    object AccessTokenResponse extends FsJsonResponsePipe[AccessTokenResponse] {
      implicit val decode: Decoder[AccessTokenResponse] = semiauto.deriveConfiguredDecoder
    }

    sealed trait SignerV2 extends Signer[OAuthVersion.V2.type]

    // FIXME: Does v2 has consumer? check Spotify
    case class AccessToken(value: String, consumer: Consumer) extends SignerV2
  }
}
