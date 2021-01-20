package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner, Scope}
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken, ResponseHandler}
import io.circe
import io.circe.generic.extras.semiauto.deriveUnwrappedDecoder
import io.circe.{Codec, Decoder, Encoder, HCursor}
import sttp.client3.circe.SttpCirceApi
import sttp.model.Uri

trait FsClientCirceApi extends SttpCirceApi {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[circe.Error, T] =
    asJson[T]

  def dropNullValues[A](encoder: Encoder[A]): Encoder[A] = encoder.mapJson(_.dropNullValues)

  implicit val sttpUriCodec: Codec[Uri] = Codec.from(
    Decoder.decodeString.emap(Uri.parse),
    Encoder.encodeString.contramap(_.toString())
  )

  implicit val accessTokenDecoder: Decoder[AccessToken] = deriveUnwrappedDecoder
  implicit val refreshTokenDecoder: Decoder[RefreshToken] = deriveUnwrappedDecoder

  implicit val accessTokenSignerDecoder: Decoder[AccessTokenSigner] = (c: HCursor) =>
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

  implicit val scopeDecoder: Decoder[Scope] =
    Decoder
      .decodeOption[String]
      .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))

  implicit val nonRefreshableTokenSignerDecoder: Decoder[NonRefreshableTokenSigner] = (c: HCursor) =>
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
