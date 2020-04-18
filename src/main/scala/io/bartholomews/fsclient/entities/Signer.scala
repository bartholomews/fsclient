package io.bartholomews.fsclient.entities

import cats.effect.Effect
import io.bartholomews.fsclient.entities.OAuthVersion.{OAuthV2, OAuthV1}
import io.bartholomews.fsclient.entities.v2.AccessTokenV2
import io.bartholomews.fsclient.requests.OAuthV2AuthorizationFramework.ClientPassword
import org.http4s.client.oauth1.{signRequest, Consumer, Token}
import org.http4s.{Request, Uri}

sealed trait Signer[+O <: OAuthVersion]

case object AuthDisabled extends Signer[Nothing]

sealed trait SignerV1 extends Signer[OAuthV1] {
  def consumer: Consumer
  private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]]
}

// Sign with consumer key/secret, but without token (i.e. not a full OAuth request)
final case class BasicSignature(consumer: Consumer) extends SignerV1 {
  override private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]] =
    signRequest(req, consumer, callback = None, verifier = None, token = None)
}

// Full OAuth v1 request
sealed trait TokenV1 extends SignerV1 {
  def token: Token
  private[fsclient] def tokenVerifier: Option[String]
  final override private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]] =
    signRequest(req, consumer, callback = None, tokenVerifier, Some(token))
}

// https://tools.ietf.org/html/rfc5849#section-2.2
final case class RequestTokenV1(token: Token, verifier: String, consumer: Consumer) extends TokenV1 {
  override private[fsclient] val tokenVerifier: Option[String] = Some(verifier)
}

// https://tools.ietf.org/html/rfc5849#section-2.3
final case class AccessTokenV1 private (token: Token, tokenVerifier: Option[String], consumer: Consumer) extends TokenV1
object AccessTokenV1 {
  def apply(token: Token, consumer: Consumer) = new AccessTokenV1(token, tokenVerifier = None, consumer)
}

final case class SignerV2(tokenEndpoint: Uri, clientPassword: ClientPassword, accessTokenResponse: AccessTokenV2)
    extends Signer[OAuthV2]
