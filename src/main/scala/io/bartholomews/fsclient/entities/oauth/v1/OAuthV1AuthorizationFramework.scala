package io.bartholomews.fsclient.entities.oauth.v1

import io.bartholomews.fsclient.codecs.ResDecoder
import io.bartholomews.fsclient.entities.oauth.AccessTokenCredentials
import io.bartholomews.fsclient.requests.FsAuthPlainText
import org.http4s.Uri

object OAuthV1AuthorizationFramework {

  sealed trait SignerType
  object SignerType {
    case object BasicSignature extends SignerType
    case object TokenSignature extends SignerType
  }

  // https://tools.ietf.org/html/rfc5849#section-2.2
  case class AccessTokenRequest(uri: Uri)(implicit decoder: ResDecoder[String, AccessTokenCredentials])
      extends FsAuthPlainText.PostEmpty[AccessTokenCredentials]
}
