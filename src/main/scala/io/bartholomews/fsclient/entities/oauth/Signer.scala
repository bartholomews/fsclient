package io.bartholomews.fsclient.entities.oauth

import cats.effect.Effect
import io.bartholomews.fsclient.codecs.FsJsonResponsePipe
import io.bartholomews.fsclient.entities.defaultConfig
import io.bartholomews.fsclient.entities.oauth.v2.OAuthV2AuthorizationFramework.{AccessToken, RefreshToken}
import io.circe.Decoder
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import org.http4s.Request
import org.http4s.client.oauth1.{signRequest, Consumer, Token}

sealed trait Signer

case object AuthDisabled extends Signer

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// OAUTH V1 SIGNER
// https://tools.ietf.org/html/rfc5849#section-1.1
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
sealed trait SignerV1 extends Signer {
  def consumer: Consumer
  private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]]
}

// Consumer-only signature
final case class ClientCredentials(consumer: Consumer) extends SignerV1 {
  override private[fsclient] def sign[F[_]: Effect](req: Request[F]): F[Request[F]] =
    signRequest(req, consumer, callback = None, verifier = None, token = None)
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

// https://tools.ietf.org/html/rfc6749#section-5.1
sealed trait SignerV2 extends Signer {
  final private val generatedAt: Long = System.currentTimeMillis()
  def accessToken: AccessToken
  // https://tools.ietf.org/html/rfc6749#section-7.1
  def tokenType: String
  def expiresIn: Long
  def maybeRefreshToken: Option[RefreshToken]
  def scope: Scope
  final def isExpired: Boolean =
    System.currentTimeMillis() > generatedAt + (expiresIn * 1000)
}

// https://tools.ietf.org/html/rfc6749#section-3.3
// TODO: consider passing a type param to `Signer` and implicit decoder for the actual application scope type
case class Scope(values: List[String])
object Scope {
  def empty: Scope = Scope(List.empty)
  implicit val decoder: Decoder[Scope] =
    Decoder.decodeString.map(str => Scope(str.split(" ").toList))
}

/*
 * https://tools.ietf.org/html/rfc6749#section-4.1.2
 * Refreshable user-level token
 */
case class AuthorizationCode(
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  refreshToken: RefreshToken,
  scope: Scope
) extends SignerV2 {
  final override val maybeRefreshToken: Option[RefreshToken] =
    Some(refreshToken)
}

object AuthorizationCode extends FsJsonResponsePipe[AuthorizationCode] {
  implicit val decoder: Decoder[AuthorizationCode] = deriveConfiguredDecoder
}

/*
 * Non-refreshable token
 *  user-level implicit grant (https://tools.ietf.org/html/rfc6749#section-4.2.2)
 *  server-level client credentials (https://tools.ietf.org/html/rfc6749#section-4.4.3)
 */
case class NonRefreshableToken(
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  scope: Scope
) extends SignerV2 {
  final override val maybeRefreshToken: Option[RefreshToken] = None
}

object NonRefreshableToken extends FsJsonResponsePipe[NonRefreshableToken] {
  implicit val decoder: Decoder[NonRefreshableToken] = deriveConfiguredDecoder
}
