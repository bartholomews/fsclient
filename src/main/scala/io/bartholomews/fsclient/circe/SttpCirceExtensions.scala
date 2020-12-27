package io.bartholomews.fsclient.circe

import io.bartholomews.fsclient.core.oauth.{AuthorizationCode, NonRefreshableToken}
import io.circe.Error
import sttp.client.circe.asJson
import sttp.client.{ResponseAs, ResponseError}

object SttpCirceExtensions {

  implicit val authorizationCodeAsJson: ResponseAs[Either[ResponseError[Error], AuthorizationCode], Nothing] =
    asJson[AuthorizationCode]

  implicit val nonRefreshableTokenAsJson: ResponseAs[Either[ResponseError[Error], NonRefreshableToken], Nothing] =
    asJson[NonRefreshableToken]
}
