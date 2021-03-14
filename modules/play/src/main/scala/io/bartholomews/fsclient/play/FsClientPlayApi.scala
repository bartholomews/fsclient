package io.bartholomews.fsclient.play

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
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import sttp.client3.playJson.asJson
import sttp.model.Uri

trait FsClientPlayApi {

  import play.api.libs.json._

  // https://www.playframework.com/documentation/latest/ScalaJsonAutomated
  implicit val defaultConfig: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit def responseHandler[T](implicit decoder: Reads[T]): ResponseHandler[JsError, T] =
    asJson[T]

  implicit val sttpUriEncoder: Writes[Uri] = (o: Uri) => JsString(o.toString)
  implicit val sttpUriDecoder: Reads[Uri] =
    _.validate[String].flatMap(str => Uri.parse(str).fold(JsError.apply, uri => JsSuccess(uri)))

  implicit val accessTokenCodec: Format[AccessToken] = Json.valueFormat[AccessToken]
  implicit val refreshTokenCodec: Format[RefreshToken] = Json.valueFormat[RefreshToken]
  implicit val clientIdCodec: Format[ClientId] = Json.valueFormat[ClientId]
  implicit val clientSecretCodec: Format[ClientSecret] = Json.valueFormat[ClientSecret]

  implicit val scopeEncoder: Writes[Scope] = Writes.list[String].contramap[Scope](_.values)
  implicit val scopeDecoder: Reads[Scope] = Reads
    .optionNoError[String]
    .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))

  private val scopeJsonPath =
    (JsPath \ "scope").read[Scope].orElse(Reads.pure(Scope(List.empty)))

  private val tokenCommonFieldsJsonPath =
    (JsPath \ "generated_at")
      .read[Long]
      .orElse(Reads.pure(System.currentTimeMillis()))
      .and((JsPath \ "access_token").read[AccessToken])
      .and((JsPath \ "token_type").read[String])
      .and((JsPath \ "expires_in").read[Long])

  implicit val accessTokenSignerEncoder: Writes[AccessTokenSigner] = Json.writes[AccessTokenSigner]
  implicit val accessTokenSignerDecoder: Reads[AccessTokenSigner] =
    tokenCommonFieldsJsonPath
      .and((JsPath \ "refresh_token").read[RefreshToken])
      .and(scopeJsonPath)(AccessTokenSigner.apply _)

  implicit val nonRefreshableTokenSignerEncoder: Writes[NonRefreshableTokenSigner] =
    Json.writes[NonRefreshableTokenSigner]
  implicit val nonRefreshableTokenSignerDecoder: Reads[NonRefreshableTokenSigner] =
    tokenCommonFieldsJsonPath
      .and(scopeJsonPath)(NonRefreshableTokenSigner.apply _)

  implicit val tokenCodec: Format[Token] = Json.format[Token]
  implicit val consumerCodec: Format[Consumer] = Json.format[Consumer]

  implicit val signatureMethodEncoder: Writes[SignatureMethod] = sig => JsString(sig.value)
  implicit val signatureMethodDecoder: Reads[SignatureMethod] = (json: JsValue) =>
    json
      .validate[String]
      .flatMap(str =>
        SignatureMethod.values
          .find(_.value == str)
          .map(s => JsSuccess.apply(s))
          .getOrElse(JsError(s"Unknown signature method: [$str]"))
      )

  implicit val accessTokenCredentialsCodec: Format[AccessTokenCredentials] =
    Json.format[AccessTokenCredentials]
  implicit val clientPasswordCodec: Format[ClientPassword] =
    Json.format[ClientPassword]
  implicit val clientPasswordAuthenticationCodec: Format[ClientPasswordAuthentication] =
    Json.format[ClientPasswordAuthentication]
  implicit val resourceOwnerAuthorizationUriCodec: Format[ResourceOwnerAuthorizationUri] =
    Json.valueFormat[ResourceOwnerAuthorizationUri]
  implicit val temporaryCredentialsCodec: Format[TemporaryCredentials] =
    Json.format[TemporaryCredentials]
}
