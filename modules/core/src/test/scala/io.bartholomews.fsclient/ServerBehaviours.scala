package io.bartholomews.fsclient

import io.bartholomews.fsclient.core.oauth.v2.OAuthV2.ResponseHandler
import org.scalatest.matchers.should.Matchers

trait ServerBehaviours[Encoder[_], Decoder[_], DE] extends Matchers {
  implicit def responseHandler[T](implicit decoder: Decoder[T]): ResponseHandler[DE, T]
}
