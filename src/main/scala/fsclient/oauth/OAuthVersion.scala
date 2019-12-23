package fsclient.oauth

import cats.effect.Effect
import fsclient.config.AppConsumer
import fsclient.defaultConfig
import fsclient.requests._
import io.circe.Decoder
import io.circe.generic.extras._
import org.http4s.client.oauth1.{signRequest, Consumer, Token}
import org.http4s.{Method, Request, Uri}

sealed trait OAuthVersion

sealed trait OAuthToken

sealed trait OAuthTokenV1 extends OAuthToken {
  def token: Token
  def verifier: Option[String]
  def consumer: Consumer
}

sealed trait OAuthTokenV2 extends OAuthToken

object OAuthVersion {
  // https://tools.ietf.org/html/rfc5849
  case object OAuthV1 extends OAuthVersion {

    case class RequestTokenV1 private (token: Token, verifier: Option[String], consumer: Consumer) extends OAuthTokenV1
    object RequestTokenV1 {
      def apply(token: Token, tokenVerifier: String)(implicit consumer: Consumer) =
        new RequestTokenV1(token, Some(tokenVerifier), consumer)
    }

    case class AccessTokenV1 private (token: Token, verifier: Option[String], consumer: Consumer) extends OAuthTokenV1
    object AccessTokenV1 {
      def apply(token: Token)(implicit consumer: Consumer) = new AccessTokenV1(token, verifier = None, consumer)
      def apply(appConsumer: AppConsumer, token: Token) =
        new AccessTokenV1(token, verifier = None, Consumer(appConsumer.key, appConsumer.secret))
    }

    private[fsclient] def sign[F[_]: Effect](v1: OAuthTokenV1)(req: Request[F]): F[Request[F]] =
      signRequest(
        req,
        v1.consumer,
        callback = None,
        v1.verifier,
        Some(v1.token)
      )

    trait AccessTokenRequestV1 extends FsClientPlainRequest {
      def token: OAuthTokenV1
    }

    // FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
    object AccessTokenRequestV1 {
      def apply(requestUri: Uri, requestToken: RequestTokenV1)(implicit consumer: Consumer): AccessTokenRequestV1 =
        new AccessTokenRequestV1 {
          override val token: OAuthTokenV1 = RequestTokenV1(requestToken.token, requestToken.verifier, consumer)
          override val uri: Uri = requestUri
          override val method: Method = Method.POST
        }
    }
  }
  // https://tools.ietf.org/html/rfc6749
  case object OAuthV2 extends OAuthVersion {
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

    case class AccessTokenV2(value: String) extends OAuthTokenV2
  }
}
