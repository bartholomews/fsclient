package io.bartholomews.fsclient.entities.oauth

import cats.effect.Effect
import io.bartholomews.fsclient.entities.oauth.v2.OAuthV2AuthorizationFramework.{
  AccessToken,
  ClientPassword,
  RefreshToken
}
import io.circe.{Decoder, HCursor}
import org.http4s.client.oauth1.{signRequest, Consumer, Token}
import org.http4s.{Request, Uri}

import scala.concurrent.duration.{Duration, _}

sealed trait Signer

case object AuthDisabled extends Signer
sealed trait OAuthSigner extends Signer

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// OAUTH V1 SIGNER
// https://tools.ietf.org/html/rfc5849#section-1.1
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
sealed trait SignerV1 extends OAuthSigner {
  def consumer: Consumer
  private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]]
}

// Consumer-only signature
final case class ClientCredentials(consumer: Consumer) extends SignerV1 {
  override private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]] =
    signRequest(req, consumer, callback = None, verifier = None, token = None)
}

// https://tools.ietf.org/html/rfc5849#page-9
case class TemporaryCredentialsRequest(consumer: Consumer, callback: Uri) extends SignerV1 {
  override private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]] =
    signRequest(req, consumer, callback = Some(callback), verifier = None, token = None)
}

// Token signature
sealed trait TokenCredentials extends SignerV1 {
  def token: Token
  private[fsclient] def tokenVerifier: Option[String]
  final override private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]] =
    signRequest(req, consumer, callback = None, tokenVerifier, Some(token))
}

// https://tools.ietf.org/html/rfc5849#section-2.2
final case class RequestTokenCredentials(token: Token, verifier: String, consumer: Consumer) extends TokenCredentials {
  override private[fsclient] val tokenVerifier: Option[String] = Some(verifier)
}

// https://tools.ietf.org/html/rfc5849#section-2.3
final case class AccessTokenCredentials private (token: Token, consumer: Consumer) extends TokenCredentials {
  override private[fsclient] val tokenVerifier: Option[String] = None
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// OAUTH V2 SIGNER
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

sealed trait SignerV2 extends OAuthSigner

// https://tools.ietf.org/html/rfc6749#section-7.1
case class ClientPasswordBasicAuthenticationV2(clientPassword: ClientPassword) extends SignerV2

// https://tools.ietf.org/html/rfc6749#section-5.1
sealed trait AccessTokenSignerV2 extends SignerV2 {
  def generatedAt: Long
  def accessToken: AccessToken
  // https://tools.ietf.org/html/rfc6749#section-7.1
  def tokenType: String
  def expiresIn: Long
  def refreshToken: Option[RefreshToken]
  def scope: Scope
  final def isExpired(threshold: Duration = 1.minute): Boolean =
    (System.currentTimeMillis() + threshold.toMillis) > generatedAt + (expiresIn * 1000)
}

// https://tools.ietf.org/html/rfc6749#section-3.3
// TODO: consider passing a type param to `Signer` and implicit decoder for the actual application scope type
case class Scope(values: List[String])
object Scope {
  def empty: Scope = Scope(List.empty)
  implicit val decoder: Decoder[Scope] =
    Decoder
      .decodeOption[String]
      .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))
}

/*
 * https://tools.ietf.org/html/rfc6749#section-4.1.2
 * Refreshable user-level token
 */
case class AuthorizationCode(
  generatedAt: Long,
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  refreshToken: Option[RefreshToken],
  scope: Scope
) extends AccessTokenSignerV2

object AuthorizationCode {
  implicit val decoder: Decoder[AuthorizationCode] = (c: HCursor) =>
    for {
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType <- c.downField("token_type").as[String]
      expiresIn <- c.downField("expires_in").as[Long]
      refreshToken <- c.downField("refresh_token").as[Option[RefreshToken]]
      scope <- c.downField("scope").as[Scope]
    } yield AuthorizationCode(
      generatedAt = System.currentTimeMillis(),
      accessToken,
      tokenType,
      expiresIn,
      refreshToken,
      scope
    )
}

/*
 * Non-refreshable token
 *  user-level implicit grant (https://tools.ietf.org/html/rfc6749#section-4.2.2)
 *  server-level client credentials (https://tools.ietf.org/html/rfc6749#section-4.4.3)
 */
case class NonRefreshableToken(
  generatedAt: Long,
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  scope: Scope
) extends AccessTokenSignerV2 {
  final override val refreshToken: Option[RefreshToken] = None
}

object NonRefreshableToken {
  implicit val decoder: Decoder[NonRefreshableToken] = (c: HCursor) =>
    for {
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType <- c.downField("token_type").as[String]
      expiresIn <- c.downField("expires_in").as[Long]
      scope <- c.downField("scope").as[Scope]
    } yield NonRefreshableToken(
      generatedAt = System.currentTimeMillis(),
      accessToken,
      tokenType,
      expiresIn,
      scope
    )
}
