package io.bartholomews.fsclient.core.oauth.v1

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.bartholomews.fsclient.core.http.FsClientSttpExtensions.mapInto
import io.bartholomews.fsclient.core.http.ResponseMapping
import io.bartholomews.fsclient.core.oauth.AccessTokenCredentials
import pureconfig.ConfigReader
import sttp.client3.{Identity, RequestT, ResponseException, emptyRequest}
import sttp.model.Uri

import java.io.ObjectInputFilter.Config
import pureconfig._

object OAuthV1 {

  sealed abstract class SignatureMethod(val value: String) extends StringEnumEntry

  object SignatureMethod extends StringEnum[SignatureMethod] {
    override val values: IndexedSeq[SignatureMethod] = findValues
    // https://tools.ietf.org/html/rfc5849#section-3.4.4
    case object PLAINTEXT extends SignatureMethod(value = "PLAINTEXT")
    // https://tools.ietf.org/html/rfc5849#section-3.4.2
    case object SHA1 extends SignatureMethod(value = "HMAC-SHA1")
  }

  // https://tools.ietf.org/html/rfc5849#section-2.2
  def accessTokenRequest[DE](
      uri: Uri
  )(implicit
      responseMapping: ResponseMapping[String, DE, AccessTokenCredentials]
  ): RequestT[Identity, Either[ResponseException[String, DE], AccessTokenCredentials], Nothing] =
    emptyRequest
      .post(uri)
      .response(mapInto[String, DE, AccessTokenCredentials])

  /** Representation of a Consumer key and secret */
  final case class Consumer(key: String, secret: String)
  object Consumer {
    implicit val configReader: ConfigReader[Consumer] =
      ConfigReader
        .forProduct2[Consumer, String, String]("key", "secret")
        .apply({ case (key, secret) => Consumer(key, secret) })
  }

  /** Representation of an OAuth Token and Token secret */
  final case class Token(value: String, secret: String)
}
