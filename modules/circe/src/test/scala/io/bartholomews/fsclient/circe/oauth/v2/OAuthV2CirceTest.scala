package io.bartholomews.fsclient.circe.oauth.v2

import io.bartholomews.fsclient.circe.{codecs, CirceServerBehaviours}
import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner}
import io.bartholomews.fsclient.core.oauth.v2.OAuthV2Test
import io.circe.Decoder

class OAuthV2CirceTest
    extends OAuthV2Test[io.circe.Encoder, io.circe.Decoder, io.circe.Error]
    with CirceServerBehaviours {

  implicit override def accessTokenSignerDecoder: Decoder[AccessTokenSigner] =
    codecs.accessTokenSignerDecoder

  implicit override def nonRefreshableTokenSignerDecoder: Decoder[NonRefreshableTokenSigner] =
    codecs.nonRefreshableTokenSignerDecoder
}
