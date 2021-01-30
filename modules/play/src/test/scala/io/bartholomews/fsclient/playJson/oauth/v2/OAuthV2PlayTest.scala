package io.bartholomews.fsclient.playJson.oauth.v2

import io.bartholomews.fsclient.core.oauth.v2.OAuthV2Test
import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner}
import io.bartholomews.fsclient.play.codecs
import io.bartholomews.fsclient.playJson.PlayServerBehaviours
import play.api.libs.json.{JsError, Reads, Writes}

class OAuthV2PlayTest extends OAuthV2Test[Writes, Reads, JsError] with PlayServerBehaviours {

  implicit override def accessTokenSignerDecoder: Reads[AccessTokenSigner] =
    codecs.accessTokenSignerDecoder

  implicit override def nonRefreshableTokenSignerDecoder: Reads[NonRefreshableTokenSigner] =
    codecs.nonRefreshableTokenSignerDecoder
}
