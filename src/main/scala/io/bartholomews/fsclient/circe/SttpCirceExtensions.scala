package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.core.oauth.{AccessTokenSigner, NonRefreshableTokenSigner}
import io.circe.Error
import sttp.client.circe.asJson
import sttp.client.{ResponseAs, ResponseError}

object SttpCirceExtensions {

  implicit val authorizationCodeAsJson: ResponseAs[Either[ResponseError[Error], AccessTokenSigner], Nothing] =
    asJson[AccessTokenSigner]

  implicit val nonRefreshableTokenAsJson: ResponseAs[Either[ResponseError[Error], NonRefreshableTokenSigner], Nothing] =
    asJson[NonRefreshableTokenSigner]
}
