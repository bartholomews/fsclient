package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, SignatureMethod, Token}
import io.bartholomews.fsclient.core.oauth.v1.TemporaryCredentials
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken, ResponseHandler}
import io.bartholomews.fsclient.core.oauth.v2.{ClientId, ClientPassword, ClientSecret}
import io.bartholomews.fsclient.core.oauth.{
  AccessTokenCredentials,
  AccessTokenSigner,
  ClientPasswordAuthentication,
  NonRefreshableTokenSigner,
  ResourceOwnerAuthorizationUri,
  Scope
}
import io.circe
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.{deriveConfiguredCodec, deriveConfiguredEncoder, deriveUnwrappedCodec}
import io.circe.{Codec, Decoder, Encoder, HCursor}
import sttp.client3.circe.SttpCirceApi
import sttp.model.Uri

trait FsClientCirceApi extends SttpCirceApi {
  implicit val defaultConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[circe.Error, T] =
    asJson[T]

  def dropNullValues[A](encoder: Encoder[A]): Encoder[A] = encoder.mapJson(_.dropNullValues)

  implicit val sttpUriCodec: Codec[Uri] = Codec.from(
    Decoder.decodeString.emap(Uri.parse),
    Encoder.encodeString.contramap(_.toString())
  )

  implicit val accessTokenCodec: Codec[AccessToken] = deriveUnwrappedCodec
  implicit val refreshTokenCodec: Codec[RefreshToken] = deriveUnwrappedCodec
  implicit val clientIdCodec: Codec[ClientId] = deriveUnwrappedCodec
  implicit val clientSecretCodec: Codec[ClientSecret] = deriveUnwrappedCodec

  implicit val scopeEncoder: Encoder[Scope] = Encoder[List[String]].contramap[Scope](_.values)
  implicit val scopeDecoder: Decoder[Scope] =
    Decoder
      .decodeOption[String]
      .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))

  implicit val accessTokenSignerEncoder: Encoder[AccessTokenSigner] = deriveConfiguredEncoder
  implicit val accessTokenSignerDecoder: Decoder[AccessTokenSigner] = (c: HCursor) =>
    for {
      generatedAt <- c.downField("generated_at").as[Option[Long]]
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType <- c.downField("token_type").as[String]
      expiresIn <- c.downField("expires_in").as[Long]
      refreshToken <- c.downField("refresh_token").as[Option[RefreshToken]]
      scope <- c.downField("scope").as[Scope]
    } yield AccessTokenSigner(
      generatedAt.getOrElse(System.currentTimeMillis()),
      accessToken,
      tokenType,
      expiresIn,
      refreshToken,
      scope
    )

  implicit val nonRefreshableTokenSignerEncoder: Encoder[NonRefreshableTokenSigner] = deriveConfiguredEncoder
  implicit val nonRefreshableTokenSignerDecoder: Decoder[NonRefreshableTokenSigner] = (c: HCursor) =>
    for {
      generatedAt <- c.downField("generated_at").as[Option[Long]]
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType <- c.downField("token_type").as[String]
      expiresIn <- c.downField("expires_in").as[Long]
      scope <- c.downField("scope").as[Scope]
    } yield NonRefreshableTokenSigner(
      generatedAt.getOrElse(System.currentTimeMillis()),
      accessToken,
      tokenType,
      expiresIn,
      scope
    )

  implicit val tokenCodec: Codec[Token] = deriveConfiguredCodec
  implicit val consumerCodec: Codec[Consumer] = deriveConfiguredCodec

  implicit val signatureMethodEncoder: Encoder[SignatureMethod] =
    Encoder.encodeString.contramap(_.value)
  implicit val signatureMethodDecoder: Decoder[SignatureMethod] =
    Decoder[String].emap(str =>
      SignatureMethod.values
        .find(_.value == str)
        .toRight(s"Unknown signature method: [$str]")
    )

  implicit val accessTokenCredentialsCodec: Codec[AccessTokenCredentials] =
    deriveConfiguredCodec
  implicit val clientPasswordCodec: Codec[ClientPassword] =
    deriveConfiguredCodec
  implicit val clientPasswordAuthenticationCodec: Codec[ClientPasswordAuthentication] =
    deriveConfiguredCodec
  implicit val resourceOwnerAuthorizationUriCodec: Codec[ResourceOwnerAuthorizationUri] =
    deriveUnwrappedCodec

  implicit val temporaryCredentialsEncoder: Encoder[TemporaryCredentials] = Encoder
    .forProduct4(
      "consumer",
      "token",
      "callback_confirmed",
      "resource_owner_authorization_uri"
    )(tc => (tc.consumer, tc.token, tc.callbackConfirmed, tc.resourceOwnerAuthorizationUri))
  implicit val temporaryCredentialsCodec: Decoder[TemporaryCredentials] = (c: HCursor) =>
    for {
      consumer <- c.downField("consumer").as[Consumer]
      token <- c.downField("token").as[Token]
      callbackConfirmed <- c.downField("callback_confirmed").as[Boolean]
      resourceOwnerAuthUri <- c.downField("resource_owner_authorization_uri").as[ResourceOwnerAuthorizationUri]
    } yield TemporaryCredentials(consumer, token, callbackConfirmed, resourceOwnerAuthUri)
}
