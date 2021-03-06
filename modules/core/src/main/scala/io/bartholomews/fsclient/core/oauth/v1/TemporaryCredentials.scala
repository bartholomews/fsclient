package io.bartholomews.fsclient.core.oauth.v1

import io.bartholomews.fsclient.core.http.ResponseMapping
import io.bartholomews.fsclient.core.oauth.ResourceOwnerAuthorizationUri
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.{Consumer, Token}
import sttp.client3.DeserializationException
import sttp.model.Uri
import sttp.model.Uri.QuerySegment

import scala.util.Try

case class TemporaryCredentials(
  consumer: Consumer,
  token: Token,
  callbackConfirmed: Boolean,
  private[fsclient] val resourceOwnerAuthorizationUri: ResourceOwnerAuthorizationUri
) {
  final val resourceOwnerAuthorizationRequest: Uri =
    resourceOwnerAuthorizationUri.value.addQuerySegment(QuerySegment.KeyValue("oauth_token", token.value))
}

object TemporaryCredentials {
  def responseMapping(
    consumer: Consumer,
    resourceOwnerAuthorizationUri: ResourceOwnerAuthorizationUri
  ): ResponseMapping[String, Exception, TemporaryCredentials] =
    ResponseMapping.plainTextTo[Exception, TemporaryCredentials] {
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
        Left(DeserializationException(other, new Exception("Unexpected response")))
    }

}
