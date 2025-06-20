package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.core.http.SttpResponses.ResponseHandler
import io.bartholomews.fsclient.core.oauth._
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, SignatureMethod, Token}
import io.bartholomews.fsclient.core.oauth.v1.TemporaryCredentials
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken}
import io.bartholomews.fsclient.core.oauth.v2.{ClientId, ClientPassword, ClientSecret}
import io.circe
import io.circe.{Codec, Decoder, Encoder, HCursor}
import sttp.client3.circe.SttpCirceApi
import sttp.model.Uri

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.util.Try

trait FsClientCirceApi extends SttpCirceApi {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[circe.Error, T] =
    asJson[T]

  private def valueClassStringCodec[A](f: String => A)(g: A => String) =
    Codec
      .from(Decoder.decodeString, Encoder.encodeString)
      .iemap(str => Right(f(str)))(g)

  def dropNullValues[A](encoder: Encoder[A]): Encoder[A] = encoder.mapJson(_.dropNullValues)

  // https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
  def localDateTimeDecoder(dateTimeFormatter: DateTimeFormatter): Decoder[LocalDateTime] =
    Decoder.decodeString.emap(str => Try(LocalDateTime.parse(str, dateTimeFormatter)).toEither.left.map(_.getMessage))

  def decodeNullableList[A](implicit decoder: Decoder[A]): Decoder[List[A]] = {
    Decoder.decodeOption[List[A]](using Decoder.decodeList[A]).map(_.getOrElse(Nil))
  }

  def decodeEmptyStringAsOption[A](implicit decoder: Decoder[A]): Decoder[Option[A]] = { (c: HCursor) =>
    c.focus match {
      case None         => Right(None)
      case Some(jValue) =>
        if (jValue.asString.contains("")) Right(None)
        else decoder(c).map(Some(_))
    }
  }

  implicit val sttpUriCodec: Codec[Uri] = Codec.from(
    Decoder.decodeString.emap(Uri.parse),
    Encoder.encodeString.contramap(_.toString)
  )

  implicit val accessTokenCodec: Codec[AccessToken] =
    Codec
      .from(Decoder.decodeString, Encoder.encodeString)
      .iemap(str => Right(AccessToken(str)))(_.value)

  implicit val refreshTokenCodec: Codec[RefreshToken] =
    valueClassStringCodec(RefreshToken.apply)(_.value)

  implicit val clientIdCodec: Codec[ClientId] =
    valueClassStringCodec(ClientId.apply)(_.value)

  implicit val clientSecretCodec: Codec[ClientSecret] =
    valueClassStringCodec(ClientSecret.apply)(_.value)

  implicit val scopeEncoder: Encoder[Scope] = Encoder[List[String]].contramap[Scope](_.values)
  implicit val scopeDecoder: Decoder[Scope] =
    Decoder
      .decodeOption[String]
      .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))

  implicit val accessTokenSignerEncoder: Encoder[AccessTokenSigner] =
    Encoder.derived[AccessTokenSigner]

  implicit val accessTokenSignerDecoder: Decoder[AccessTokenSigner] = (c: HCursor) =>
    for {
      generatedAt  <- c.downField("generated_at").as[Option[Long]]
      accessToken  <- c.downField("access_token").as[AccessToken]
      tokenType    <- c.downField("token_type").as[String]
      expiresIn    <- c.downField("expires_in").as[Long]
      refreshToken <- c.downField("refresh_token").as[RefreshToken]
      scope        <- c.downField("scope").as[Scope]
    } yield AccessTokenSigner(
      generatedAt.getOrElse(System.currentTimeMillis()),
      accessToken,
      tokenType,
      expiresIn,
      Some(refreshToken),
      scope
    )

  implicit val nonRefreshableTokenSignerEncoder: Encoder[NonRefreshableTokenSigner] =
    Encoder.derived[NonRefreshableTokenSigner]

  implicit val nonRefreshableTokenSignerDecoder: Decoder[NonRefreshableTokenSigner] = (c: HCursor) =>
    for {
      generatedAt <- c.downField("generated_at").as[Option[Long]]
      accessToken <- c.downField("access_token").as[AccessToken]
      tokenType   <- c.downField("token_type").as[String]
      expiresIn   <- c.downField("expires_in").as[Long]
      scope       <- c.downField("scope").as[Scope]
    } yield NonRefreshableTokenSigner(
      generatedAt.getOrElse(System.currentTimeMillis()),
      accessToken,
      tokenType,
      expiresIn,
      scope
    )

  implicit val tokenCodec: Codec[Token] =
    Codec.forProduct2[Token, String, String]("value", "secret")({ case (a, b) =>
      Token(a, b)
    })(t => (t._1, t._2))

  implicit val consumerCodec: Codec[Consumer] =
    Codec.forProduct2[Consumer, String, String]("key", "secret")({ case (a, b) =>
      Consumer(a, b)
    })(t => (t._1, t._2))

  implicit val signatureMethodEncoder: Encoder[SignatureMethod] =
    Encoder.encodeString.contramap(_.value)
  implicit val signatureMethodDecoder: Decoder[SignatureMethod] =
    Decoder[String].emap(str =>
      SignatureMethod.values
        .find(_.value == str)
        .toRight(s"Unknown signature method: [$str]")
    )

  implicit val accessTokenCredentialsCodec: Codec[AccessTokenCredentials] =
    Codec.derived[AccessTokenCredentials]

  implicit val clientPasswordCodec: Codec[ClientPassword] =
    Codec.derived[ClientPassword]

  implicit val clientPasswordAuthenticationCodec: Codec[ClientPasswordAuthentication] =
    Codec.derived[ClientPasswordAuthentication]

  implicit val resourceOwnerAuthorizationUriCodec: Codec[ResourceOwnerAuthorizationUri] =
    sttpUriCodec
      .iemap(str => Right(ResourceOwnerAuthorizationUri(str)))(
        _.value
      )

  implicit val temporaryCredentialsEncoder: Encoder[TemporaryCredentials] = Encoder
    .forProduct4(
      "consumer",
      "token",
      "callback_confirmed",
      "resource_owner_authorization_uri"
    )(tc => (tc.consumer, tc.token, tc.callbackConfirmed, tc.resourceOwnerAuthorizationUri))

  implicit val temporaryCredentialsCodec: Decoder[TemporaryCredentials] = (c: HCursor) =>
    for {
      consumer             <- c.downField("consumer").as[Consumer]
      token                <- c.downField("token").as[Token]
      callbackConfirmed    <- c.downField("callback_confirmed").as[Boolean]
      resourceOwnerAuthUri <- c.downField("resource_owner_authorization_uri").as[ResourceOwnerAuthorizationUri]
    } yield TemporaryCredentials(consumer, token, callbackConfirmed, resourceOwnerAuthUri)
}
