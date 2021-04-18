package io.bartholomews.fsclient.playJson

import io.bartholomews.fsclient.core.http.SttpResponses.ResponseHandler
import io.bartholomews.fsclient.play.codecs
import play.api.libs.json.{JsError, Reads}

trait PlayServerBehaviours {
  implicit def responseHandler[T](implicit decoder: Reads[T]): ResponseHandler[JsError, T] =
    codecs.responseHandler
}
