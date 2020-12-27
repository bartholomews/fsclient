package io.bartholomews.fsclient.core.oauth.v1

import io.bartholomews.fsclient.core.http.FromPlainText
import io.bartholomews.fsclient.core.oauth.ResourceOwnerAuthorizationUri
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, Token}
import sttp.client.DeserializationError
import sttp.model.Uri
import sttp.model.Uri.QuerySegment

import scala.util.Try

case class TemporaryCredentials(
  consumer: Consumer,
  token: Token,
  callbackConfirmed: Boolean,
  private val resourceOwnerAuthorizationUri: ResourceOwnerAuthorizationUri
) {
  final val resourceOwnerAuthorizationRequest: Uri =
    resourceOwnerAuthorizationUri.value.querySegment(QuerySegment.KeyValue("oauth_token", token.value))
}

object TemporaryCredentials {

  def fromPlainText(
    consumer: Consumer,
    resourceOwnerAuthorizationUri: ResourceOwnerAuthorizationUri
  ): FromPlainText[TemporaryCredentials] = {
    case s"oauth_token=$token&oauth_token_secret=$secret&oauth_callback_confirmed=$flag" =>
      Right(
        TemporaryCredentials(
          consumer = consumer,
          token = Token(token, secret),
          callbackConfirmed = Try(flag.toBoolean).getOrElse(false),
          resourceOwnerAuthorizationUri
        )
      )

    case other =>
      Left(DeserializationError(other, new Exception("Unexpected response")))
  }
}
