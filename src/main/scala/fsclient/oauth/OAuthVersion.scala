package fsclient.oauth

import cats.effect.Effect
import fsclient.defaultConfig
import fsclient.entities._
import io.circe.generic.extras.ConfiguredJsonCodec
import org.http4s.client.oauth1.{signRequest, Consumer}
import org.http4s.{Method, Request, Uri}

sealed trait OAuthVersion

object OAuthVersion {
  // https://tools.ietf.org/html/rfc5849
  case object OAuthV1 extends OAuthVersion {
    private[fsclient] def sign[F[_]: Effect](consumer: Consumer, oAuthInfo: OAuthInfo)(req: Request[F]): F[Request[F]] =
      oAuthInfo match {
        case Basic(_) =>
          signRequest(
            req,
            consumer,
            callback = None,
            verifier = None,
            token = None
          )

        case OAuthTokenInfo(_, oAuthToken) =>
          signRequest(
            req,
            consumer,
            callback = None,
            oAuthToken.verifier,
            Some(oAuthToken.token)
          )
      }

    // FIXME: If this is not a standard oAuth request, should be constructed client-side: double check RFC
    object AccessTokenRequest {
      def apply(requestUri: Uri, requestToken: RequestToken): AccessTokenRequest =
        new AccessTokenRequest {
          override val token = OAuthToken(requestToken.token, Some(requestToken.verifier))
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
  }
}
