package fsclient.mocks.server

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import fsclient.mocks.MockClientConfig
import WiremockUtils._
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import org.apache.http.entity.ContentType

case object AuthenticatedRequestTransformer extends ResponseDefinitionTransformer
  with OAuthServer with MockClientConfig {

  override val applyGlobally = false

  override def transform(request: Request,
                         response: ResponseDefinition,
                         files: FileSource,
                         parameters: Parameters): ResponseDefinition = {

    val textPlainResponse = ResponseDefinitionBuilder.like(response).but()
      .withHeader(`Content-Type`, ContentType.TEXT_PLAIN.getMimeType)

    oAuthRequestHeaders(request) match {

      case accessTokenResponseRegex(_, key, _, _, _, _, _) =>
        if (key == validConsumerKey) textPlainResponse.withStatus(200).build()
        else if (key == invalidConsumerKey) response.error(401, ErrorMessage.invalidSignature)
        // FIXME this won't work chained with another transformer which will change the body (e.g. json response):
        //        else if (key == consumerGettingUnexpectedResponse) res.withBody(unexpectedResponse).build()
        else response.error(401, ErrorMessage.invalidConsumer)

      case _ => textPlainResponse.withStatus(400).build()
    }
  }

  override def getName: String = "oauth-request-transformer"
}