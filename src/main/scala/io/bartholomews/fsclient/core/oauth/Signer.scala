package io.bartholomews.fsclient.core.oauth

import cats.implicits.toBifunctorOps
import io.bartholomews.fsclient.core.config.UserAgent
import io.bartholomews.fsclient.core.http.FsClientSttpExtensions.{mapInto, PartialRequestExtensions, RequestExtensions}
import io.bartholomews.fsclient.core.http.ResponseMapping
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, SignatureMethod, Token}
import io.bartholomews.fsclient.core.oauth.v1.Signatures.{authorization, makeOAuthParams}
import io.bartholomews.fsclient.core.oauth.v1.{Signatures, TemporaryCredentials}
import io.bartholomews.fsclient.core.oauth.v2.ClientPassword
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken}
import io.circe.{Decoder, HCursor}
import sttp.client.{emptyRequest, DeserializationError, Request, Response, ResponseError, SttpBackend}
import sttp.model.Uri

import scala.concurrent.duration.{Duration, DurationInt}

sealed trait Signer

case object AuthDisabled extends Signer
sealed trait OAuthSigner extends Signer {
  def sign[T, S](request: Request[T, S]): Request[T, S]
}

final case class CustomAuthorizationHeader(value: String) extends OAuthSigner {
  override def sign[T, S](request: Request[T, S]): Request[T, S] =
    request.header(Signatures.authorization(value))
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// OAUTH V1 SIGNER
// https://tools.ietf.org/html/rfc5849#section-1.1
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
sealed trait SignerV1 extends OAuthSigner {
  private[fsclient] def signatureMethod: SignatureMethod
  private[fsclient] def maybeToken: Option[Token]
  def consumer: Consumer
  final override def sign[T, S](request: Request[T, S]): Request[T, S] =
    request.header(authorization(s"OAuth ${makeOAuthParams(signer = this, request).mkString(", ")}"))
}

final case class ClientCredentials(consumer: Consumer, signatureMethod: SignatureMethod) extends SignerV1 {
  override private[fsclient] val maybeToken: Option[Token] = None
}

object ClientCredentials {
  def apply(consumer: Consumer): ClientCredentials = new ClientCredentials(
    consumer,
    SignatureMethod.SHA1
  )
}

case class CallbackUri(value: Uri) extends AnyVal
case class ResourceOwnerAuthorizationUri(value: Uri) extends AnyVal

// https://tools.ietf.org/html/rfc5849#page-9
// it returns a `TemporaryCredentials`
final case class TemporaryCredentialsRequest(
  consumer: Consumer,
  callbackUri: CallbackUri,
  signatureMethod: SignatureMethod
) extends SignerV1 {

  override private[fsclient] val maybeToken: Option[Token] = None

  def send[F[_], S <: SignerV1](
    temporaryCredentialsRequestUri: Uri,
    userAgent: UserAgent,
    resourceOwnerAuthorizationUri: ResourceOwnerAuthorizationUri
  )(implicit
    backend: SttpBackend[F, Nothing, Nothing]
  ): F[Response[Either[ResponseError[Exception], TemporaryCredentials]]] = {
    implicit val responseMapping: ResponseMapping[String, TemporaryCredentials] =
      TemporaryCredentials.responseMapping(consumer, resourceOwnerAuthorizationUri)

    emptyRequest
      .get(temporaryCredentialsRequestUri)
      .sign(this)
      .userAgent(userAgent)
      .response(mapInto[String, TemporaryCredentials])
      .send()
  }
}

object TemporaryCredentialsRequest {
  def apply(consumer: Consumer, callbackUri: CallbackUri): TemporaryCredentialsRequest =
    new TemporaryCredentialsRequest(
      consumer,
      callbackUri,
      SignatureMethod.SHA1
    )
}

// Token signature
sealed trait TokenCredentials extends SignerV1 {
  def token: Token
  final override private[fsclient] val maybeToken = Some(token)
  private[fsclient] def tokenVerifier: Option[String]
}

// https://tools.ietf.org/html/rfc5849#section-2.2
final case class RequestTokenCredentials(
  token: Token,
  verifier: String,
  consumer: Consumer,
  signatureMethod: SignatureMethod
) extends TokenCredentials {
  override private[fsclient] val tokenVerifier: Option[String] = Some(verifier)
}

object RequestTokenCredentials {
  /*
   callbackUri MUST be updated and have oauth_token and oauth_verifier
   parameters as per https://tools.ietf.org/html/rfc5849#section-2.2
   */
  def apply(
    callbackUri: CallbackUri,
    temporaryCredentials: TemporaryCredentials
  ): Either[DeserializationError[Exception], RequestTokenCredentials] = apply(
    callbackUri,
    temporaryCredentials,
    SignatureMethod.SHA1
  )

  /*
   callbackUri MUST be updated and have oauth_token and oauth_verifier
   parameters as per https://tools.ietf.org/html/rfc5849#section-2.2
   */
  def apply(
    callbackUri: CallbackUri,
    temporaryCredentials: TemporaryCredentials,
    signatureMethod: SignatureMethod
  ): Either[DeserializationError[Exception], RequestTokenCredentials] = {

    val queryParams = callbackUri.value.paramsSeq
    val callbackResponse: Either[String, String] = queryParams
      .collectFirst({
        case ("denied", _) => Left("permission_denied")
        case ("oauth_token", token) =>
          if (token != temporaryCredentials.token.value) Left("oauth_token_mismatch")
          else
            queryParams
              .collectFirst({ case ("oauth_verifier", verifier) => Right(verifier) })
              .toRight("missing_oauth_verifier_query_parameter")
              .joinRight
      })
      .toRight("missing_required_query_parameters")
      .joinRight

    callbackResponse.bimap(
      errorMsg =>
        DeserializationError(errorMsg, new Exception("Callback uri cannot be used to request the access token")),
      verifier =>
        RequestTokenCredentials(temporaryCredentials.token, verifier, temporaryCredentials.consumer, signatureMethod)
    )
  }
}

// https://tools.ietf.org/html/rfc5849#section-2.3
final case class AccessTokenCredentials private (token: Token, consumer: Consumer, signatureMethod: SignatureMethod)
    extends TokenCredentials {
  override private[fsclient] val tokenVerifier: Option[String] = None
}

object AccessTokenCredentials {
  def responseMapping(
    consumer: Consumer,
    signatureMethod: SignatureMethod
  ): ResponseMapping[String, AccessTokenCredentials] =
    ResponseMapping.plainTextTo[AccessTokenCredentials] {
      case s"oauth_token=$token&oauth_token_secret=$secret" =>
        Right(AccessTokenCredentials(Token(token, secret), consumer, signatureMethod))
      case other =>
        Left(DeserializationError(other, new Exception("Unexpected response")))
    }
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// OAUTH V2 SIGNER
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

sealed trait SignerV2 extends OAuthSigner

// https://tools.ietf.org/html/rfc6749#section-7.1
final case class ClientPasswordAuthentication(clientPassword: ClientPassword) extends SignerV2 {
  override def sign[T, S](request: Request[T, S]): Request[T, S] =
    request.auth.basic(clientPassword.clientId.value, clientPassword.clientSecret.value)
}

// https://tools.ietf.org/html/rfc6749#section-5.1
sealed trait TokenSignerV2 extends SignerV2 {
  def generatedAt: Long
  def accessToken: AccessToken
  // https://tools.ietf.org/html/rfc6749#section-7.1
  def tokenType: String
  def expiresIn: Long
  def refreshToken: Option[RefreshToken]
  def scope: Scope
  final def isExpired(threshold: Duration = 1.minute): Boolean =
    (System.currentTimeMillis() + threshold.toMillis) > generatedAt + (expiresIn * 1000)

  final override def sign[T, S](request: Request[T, S]): Request[T, S] =
    tokenType.toUpperCase match {
      case "BEARER" =>
        // logger.debug("Signing request with OAuth v2 Bearer...")
        request.auth.bearer(accessToken.value)
      case _ =>
        // logger.warn(s"Unknown token type [$other]: The request will not be signed")
        request
    }
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
final case class AccessTokenSigner(
  generatedAt: Long,
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  refreshToken: Option[RefreshToken],
  scope: Scope
) extends TokenSignerV2

object AccessTokenSigner {
  implicit val decoder: Decoder[AccessTokenSigner] = (c: HCursor) =>
    for {
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType <- c.downField("token_type").as[String]
      expiresIn <- c.downField("expires_in").as[Long]
      refreshToken <- c.downField("refresh_token").as[Option[RefreshToken]]
      scope <- c.downField("scope").as[Scope]
    } yield AccessTokenSigner(
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
final case class NonRefreshableTokenSigner(
  generatedAt: Long,
  accessToken: AccessToken,
  tokenType: String,
  expiresIn: Long,
  scope: Scope
) extends TokenSignerV2 {
  override val refreshToken: Option[RefreshToken] = None
}

object NonRefreshableTokenSigner {
  implicit val decoder: Decoder[NonRefreshableTokenSigner] = (c: HCursor) =>
    for {
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType <- c.downField("token_type").as[String]
      expiresIn <- c.downField("expires_in").as[Long]
      scope <- c.downField("scope").as[Scope]
    } yield NonRefreshableTokenSigner(
      generatedAt = System.currentTimeMillis(),
      accessToken,
      tokenType,
      expiresIn,
      scope
    )
}
