package fsclient.mocks.server

import com.github.tomakehurst.wiremock.http.Request

import scala.util.matching.Regex

trait OAuthServer {

  def oAuthRequestHeaders(request: Request): String = request.getHeader("Authorization")

  val oAuthResponseRegexStr: String =
    "OAuth oauth_signature=\"(.*)\"," +
      "oauth_consumer_key=\"(.*)\"," +
      "oauth_signature_method=\"(.*)\"," +
      "oauth_timestamp=\"(.*)\"," +
      "oauth_nonce=\"(.*)\"," +
      "oauth_version=\"(.*)\"," +
      "oauth_callback=\"(.*)\""

  val accessTokenResponseRegex: Regex = "Access token response: token=\"(.*)\", secret=\"(.*)\"".r
  val requestTokenResponseRegex: Regex = (oAuthResponseRegexStr + ",oauth_verifier=\"(.*)\"").r

  object ErrorMessage {
    def invalidRequestToken(token: String) = s"Invalid request token: $token"

    val invalidConsumer = "Invalid consumer."
    val invalidSignature = "Invalid signature. This additional text shouldn't be shown."
    val invalidVerifier: String = "Unable to retrieve access token. " +
      "Your request token may have expired."
  }

}
