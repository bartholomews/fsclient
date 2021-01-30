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
  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit def responseHandler[T](implicit decoder: Reads[T]): ResponseHandler[JsError, T] =
    asJson[T]

  implicit val accessTokenEncoder: Writes[AccessToken] = Json.valueWrites[AccessToken]
  implicit val accessTokenDecoder: Reads[AccessToken] = Json.valueReads[AccessToken]
  implicit val refreshTokenEncoder: Writes[RefreshToken] = Json.valueWrites[RefreshToken]
  implicit val refreshTokenDecoder: Reads[RefreshToken] = Json.valueReads[RefreshToken]
  implicit val clientIdEncoder: Writes[ClientId] = Json.valueWrites[ClientId]
  implicit val clientIdDecoder: Reads[ClientId] = Json.valueReads[ClientId]
  implicit val clientSecretEncoder: Writes[ClientSecret] = Json.valueWrites[ClientSecret]
  implicit val clientSecretDecoder: Reads[ClientSecret] = Json.valueReads[ClientSecret]

  implicit val scopeEncoder: Writes[Scope] = Writes.list[String].contramap[Scope](_.values)
  implicit val scopeDecoder: Reads[Scope] = Reads
    .optionNoError[String]
    .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))

  private val scopeJsonPath =
    (JsPath \ "scope").read[Scope].orElse(Reads.pure(Scope(List.empty)))

  private val tokenCommonFieldsJsonPath =
    (JsPath \ "generated_at")
      .read[Long]
      .orElse(Reads.pure(System.nanoTime()))
      .and((JsPath \ "access_token").read[AccessToken])
      .and((JsPath \ "token_type").read[String])
      .and((JsPath \ "expires_in").read[Long])

  implicit val authorizationTokenSignerEncoder: Writes[AccessTokenSigner] = Json.writes[AccessTokenSigner]
  implicit val accessTokenSignerDecoder: Reads[AccessTokenSigner] =
    tokenCommonFieldsJsonPath
      .and((JsPath \ "refresh_token").readNullable[RefreshToken])
      .and(scopeJsonPath)(AccessTokenSigner.apply _)

  implicit val nonRefreshableTokenSignerEncoder: Writes[NonRefreshableTokenSigner] =
    Json.writes[NonRefreshableTokenSigner]
  implicit val nonRefreshableTokenSignerDecoder: Reads[NonRefreshableTokenSigner] =
    tokenCommonFieldsJsonPath
      .and(scopeJsonPath)(NonRefreshableTokenSigner.apply _)

  implicit val uriEncoder: Writes[Uri] = (o: Uri) => JsString(o.toString)
  implicit val uriDecoder: Reads[Uri] =
    _.validate[String].flatMap(str => Uri.parse(str).fold(JsError.apply, uri => JsSuccess(uri)))

  implicit val tokenEncoder: Writes[Token] = Json.writes[Token]
  implicit val tokenDecoder: Reads[Token] =
    (JsPath \ "value").read[String].and((JsPath \ "secret").read[String])(Token.apply _)

  implicit val consumerEncoder: Writes[Consumer] = Json.writes[Consumer]
  implicit val consumerDecoder: Reads[Consumer] =
    (JsPath \ "key").read[String].and((JsPath \ "secret").read[String])(Consumer.apply _)

  implicit val signatureMethodEncoder: Writes[SignatureMethod] = sig => JsString(sig.value)
  implicit val signatureMethodDecoder: Reads[SignatureMethod] = (json: JsValue) =>
    json.validate[String].flatMap {
      case SignatureMethod.PLAINTEXT.value => JsSuccess(SignatureMethod.PLAINTEXT)
      case SignatureMethod.SHA1.value      => JsSuccess(SignatureMethod.SHA1)
      case other                           => JsError(s"Unknown signature method: [$other]")
    }

  implicit val accessTokenCredentialsEncoder: Writes[AccessTokenCredentials] =
    Json.writes[AccessTokenCredentials]
  implicit val accessTokenCredentialsDecoder: Reads[AccessTokenCredentials] =
    Json.reads[AccessTokenCredentials]

  implicit val clientPasswordEncoder: Writes[ClientPassword] = Json.writes[ClientPassword]
  implicit val clientPasswordDecoder: Reads[ClientPassword] = Json.reads[ClientPassword]

  implicit val clientPasswordAuthenticationEncoder: Writes[ClientPasswordAuthentication] =
    Json.writes[ClientPasswordAuthentication]
  implicit val clientPasswordAuthenticationDecoder: Reads[ClientPasswordAuthentication] =
    Json.reads[ClientPasswordAuthentication]

  implicit val resourceOwnerAuthorizationUriEncoder: Writes[ResourceOwnerAuthorizationUri] =
    Json.valueWrites[ResourceOwnerAuthorizationUri]
  implicit val resourceOwnerAuthorizationUriDecoder: Reads[ResourceOwnerAuthorizationUri] =
    Json.valueReads[ResourceOwnerAuthorizationUri]

  implicit val temporaryCredentialsEncoder: Writes[TemporaryCredentials] = Json.writes[TemporaryCredentials]
  implicit val temporaryCredentialsDecoder: Reads[TemporaryCredentials] = Json.reads[TemporaryCredentials]
}
