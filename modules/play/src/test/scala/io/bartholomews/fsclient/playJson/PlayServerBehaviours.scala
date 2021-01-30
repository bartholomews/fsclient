package io.bartholomews.fsclient.playJson

import io.bartholomews.fsclient.ServerBehaviours
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ResponseHandler
import io.bartholomews.fsclient.play.codecs
import play.api.libs.json.{JsError, Reads, Writes}

trait PlayServerBehaviours extends ServerBehaviours[Writes, Reads, JsError] {
  implicit def responseHandler[T](implicit decoder: Reads[T]): ResponseHandler[JsError, T] =
    codecs.responseHandler
}
