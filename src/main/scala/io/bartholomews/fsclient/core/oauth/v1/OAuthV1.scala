package io.bartholomews.fsclient.core.oauth.v1

import io.bartholomews.fsclient.core.http.FromPlainText
import io.bartholomews.fsclient.core.oauth.AccessTokenCredentials
import sttp.client.{emptyRequest, Identity, RequestT, ResponseError}
import sttp.model.Uri

object OAuthV1 {

  import io.bartholomews.fsclient.core.http.FsClientSttpExtensions._

  sealed trait SignatureMethod {
    def value: String
  }

  object SignatureMethod {
    // https://tools.ietf.org/html/rfc5849#section-3.4.4
    case object PLAINTEXT extends SignatureMethod {
      final override val value: String = "PLAINTEXT"
    }

    // https://tools.ietf.org/html/rfc5849#section-3.4.2
    case object SHA1 extends SignatureMethod {
      final override val value: String = "HMAC-SHA1"
    }
  }

  // https://tools.ietf.org/html/rfc5849#section-2.2
  def accessTokenRequest(
    uri: Uri
  )(implicit
    fromString: FromPlainText[AccessTokenCredentials]
  ): RequestT[Identity, Either[ResponseError[Exception], AccessTokenCredentials], Nothing] =
    emptyRequest
      .post(uri)
      .response(asStringMappedInto[AccessTokenCredentials])

  /** Representation of a Consumer key and secret */
  final case class Consumer(key: String, secret: String)

  /** Representation of an OAuth Token and Token secret */
  final case class Token(value: String, secret: String)
}
