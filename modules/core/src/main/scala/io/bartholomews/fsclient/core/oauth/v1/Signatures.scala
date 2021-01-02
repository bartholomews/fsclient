package io.bartholomews.fsclient.core.oauth.v1

import java.nio.charset.StandardCharsets

import io.bartholomews.fsclient.core.http.FsClientSttpExtensions
import io.bartholomews.fsclient.core.oauth.v1.OAuthV1.SignatureMethod
import io.bartholomews.fsclient.core.oauth.{ClientCredentials, SignerV1, TemporaryCredentialsRequest, TokenCredentials}
import javax.crypto
import sttp.client.{Request, StringBody}
import sttp.model.internal.Rfc3986
import sttp.model.internal.Rfc3986.Unreserved
import sttp.model.{Header, MediaType}

object Signatures {

  import FsClientSttpExtensions._

  private[fsclient] def authorization(value: String): Header = Header("Authorization", value)

  private val SHA1 = "HmacSHA1"

  private def bytes(str: String) = str.getBytes(StandardCharsets.UTF_8)

  // This is based on from org.http4s.oauth1
  // https://tools.ietf.org/html/rfc5849#section-3.4.2
  private def makeSHASig(text: String, key: String): String = {
    val sha1 = crypto.Mac.getInstance(SHA1)
    //val key = encode(consumerSecret) + "&" + tokenSecret.map(t => encode(t)).getOrElse("")
    sha1.init(new crypto.spec.SecretKeySpec(bytes(key), SHA1))
    val sigBytes = sha1.doFinal(bytes(text))
    java.util.Base64.getEncoder.encodeToString(sigBytes)
  }

  // https://tools.ietf.org/html/rfc5849#section-3.2
  private[fsclient] def makeOAuthParams[T, S](signer: SignerV1, request: Request[T, S]): List[String] = {

    val key =
      s"${signer.consumer.secret}&${signer.maybeToken.map(_.secret).getOrElse("")}"

    // https://tools.ietf.org/html/rfc5849#section-3.6
    def encode(str: String): String = Rfc3986.encode(Unreserved)(str)

    val oAuthHeadersFields: List[(String, String)] = List(
      ("oauth_version", "1.0"),
      ("oauth_consumer_key", signer.consumer.key),
      ("oauth_nonce", System.nanoTime.toString),
      ("oauth_signature_method", signer.signatureMethod.value),
      ("oauth_timestamp", (System.currentTimeMillis / 1000).toString)
    ) ++ (signer match {
      case _: ClientCredentials => List.empty
      case c: TemporaryCredentialsRequest =>
        List(("oauth_callback", encode(c.redirectUri.value.toString())))
      case c: TokenCredentials =>
        Tuple2("oauth_token", c.token.value) ::
          c.tokenVerifier.toList.map(verifier => ("oauth_verifier", verifier))
    })

    val entityBodyParams: List[(String, String)] = request.body match {
      case body: StringBody =>
        if (!request.hasContentType(MediaType.ApplicationXWwwFormUrlencoded)) List.empty
        else
          body.s
            .split("&")
            .collect {
              case s"$k=$v" => (k, v)
              case k        => (k, "")
            }
            .toList
      case _ => List.empty
    }

    val signature = signer.signatureMethod match {
      case SignatureMethod.PLAINTEXT => s"oauth_signature=$key"
      case SignatureMethod.SHA1      =>
        // https://tools.ietf.org/html/rfc5849#section-3.4.1.1
        val method = request.method.method
        val baseUri = encode(request.uri.copy(querySegments = Seq.empty).toString())
        // https://tools.ietf.org/html/rfc5849#section-3.4.1.3.2
        val params: String = encode {
          (request.uri.paramsSeq ++ oAuthHeadersFields ++ entityBodyParams).sorted
            .map({ case (k, v) => s"$k=$v" })
            .mkString("&")
        }

        val text = s"$method&$baseUri&$params"
        s"oauth_signature=${makeSHASig(text, key)}"
    }

    oAuthHeadersFields.map({ case (k, v) => s"$k=$v" }) :+ signature
  }
}
