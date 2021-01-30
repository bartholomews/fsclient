package io.bartholomews.fsclient.play

import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.{AccessToken, RefreshToken, ResponseHandler}
import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner, Scope}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsError, JsPath, JsString, JsSuccess, Json, Reads, Writes}
import sttp.client3.playJson.asJson
import sttp.model.Uri

trait FsClientPlayApi {
  implicit def responseHandler[T](implicit decoder: Reads[T]): ResponseHandler[JsError, T] =
    asJson[T]

  implicit val accessTokenDecoder: Reads[AccessToken] = Json.valueReads
  implicit val refreshTokenDecoder: Reads[RefreshToken] = Json.valueReads

  implicit val scopeDecoder: Reads[Scope] = Reads
    .optionNoError[String]
    .map(_.fold(Scope(List.empty))(str => Scope(str.split(" ").toList)))

  private val tokenCommonFieldsJsonPath =
    (JsPath \ "generated_at")
      .read[Long]
      .orElse(Reads.pure(System.currentTimeMillis))
      .and((JsPath \ "access_token").read[AccessToken])
      .and((JsPath \ "token_type").read[String])
      .and((JsPath \ "expires_in").read[Long])

  private val scopeJsonPath =
    (JsPath \ "scope").read[Scope].orElse(Reads.pure(Scope(List.empty)))

  implicit val accessTokenSignerDecoder: Reads[AccessTokenSigner] =
    tokenCommonFieldsJsonPath
      .and((JsPath \ "refresh_token").readNullable[RefreshToken])
      .and(scopeJsonPath)(AccessTokenSigner.apply _)

  implicit val nonRefreshableTokenSignerDecoder: Reads[NonRefreshableTokenSigner] =
    tokenCommonFieldsJsonPath
      .and(scopeJsonPath)(NonRefreshableTokenSigner.apply _)

  implicit val uriEncoder: Writes[Uri] = (o: Uri) => JsString(o.toString)
  implicit val uriDecoder: Reads[Uri] = {
    case JsString(value) => Uri.parse(value).fold(JsError.apply, uri => JsSuccess(uri))
    case other           => JsError(s"Expected a json string, got [$other]")
  }
}
