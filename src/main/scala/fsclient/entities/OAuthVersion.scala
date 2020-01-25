package fsclient.entities

import cats.effect.Effect
import fsclient.codecs.FsJsonResponsePipe
import fsclient.defaultConfig
import io.circe.Decoder
import io.circe.generic.extras._
import org.http4s.Request
import org.http4s.client.oauth1.{signRequest, Consumer, Token}

sealed trait AuthVersion

sealed trait Signer {
  def consumer: Consumer
}

object AuthVersion {
  // https://tools.ietf.org/html/rfc5849 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  case object V1 extends AuthVersion {

    // Sign with consumer key/secret, but without token (i.e. not a full OAuth request)
    case class BasicSignature(consumer: Consumer) extends Signer

    // Full OAuth v1 request
    sealed trait OAuthToken extends Signer {
      def token: Token
      def verifier: Option[String]
    }

    case class RequestToken private (token: Token, verifier: Option[String], consumer: Consumer) extends OAuthToken
    object RequestToken {
      def apply(token: Token, tokenVerifier: String)(implicit consumer: Consumer) =
        new RequestToken(token, Some(tokenVerifier), consumer)
    }

    case class AccessToken private (token: Token, verifier: Option[String], consumer: Consumer) extends OAuthToken
    object AccessToken {
      def apply(token: Token)(implicit consumer: Consumer) = new AccessToken(token, verifier = None, consumer)
    }

    private[fsclient] def sign[F[_]: Effect](v1: BasicSignature)(req: Request[F]): F[Request[F]] =
      signRequest(req, v1.consumer, callback = None, verifier = None, token = None)

    private[fsclient] def sign[F[_]: Effect](v1: OAuthToken)(req: Request[F]): F[Request[F]] =
      signRequest(req, v1.consumer, callback = None, v1.verifier, Some(v1.token))
  }
  // https://tools.ietf.org/html/rfc6749 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  case object V2 extends AuthVersion {
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

    sealed trait OAuthToken extends Signer

    // FIXME: Does v2 has consumer? check Spotify
    case class AccessToken(value: String, consumer: Consumer) extends OAuthToken
  }
}
