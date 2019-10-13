package fsclient.mocks.server

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import fsclient.mocks.MockClientConfig
import WiremockUtils._

object ValidateTokenRequestBodyTransformer
    extends ResponseDefinitionTransformer
    with MockClientConfig
    with OAuthServer {

  override val applyGlobally = false

  override def transform(request: Request,
                         response: ResponseDefinition,
                         files: FileSource,
                         parameters: Parameters): ResponseDefinition = {

    def validateVerifier: ResponseDefinition = oAuthRequestHeaders(request) match {
      case requestTokenResponseRegex(_, _, _, _, _, _, _, verifier) =>
//        if(verifier == emptyResponseMock) likeResponse.withBody("").build()
        if (verifier == validOAuthVerifier) response
        else response.error(401, ErrorMessage.invalidVerifier)

      case _ => response.error(401, ErrorMessage.invalidSignature)
    }

    val requestBody = "oauth_token=(.*)&oauth_token_secret=(.*)".r

    response.getBody match {

      case requestBody(token, secret) =>
        if (token != validOAuthTokenValue) response.error(401, ErrorMessage.invalidRequestToken(token))
        else if (secret != validOAuthTokenSecret) response.error(401, ErrorMessage.invalidSignature)
        else validateVerifier

      case _ => response.error(400, "")
    }
  }

  override def getName: String = "validate-token-request-body-transformer"
}
