package fsclient.mocks.server

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import fsclient.mocks.MockEndpoints
import org.apache.http.entity.ContentType
import WiremockUtils._

case object ResourcePlainTextTransformer extends ResponseDefinitionTransformer with MockEndpoints {

  override val applyGlobally = false

  override def transform(request: Request,
                         response: ResponseDefinition,
                         files: FileSource,
                         parameters: Parameters): ResponseDefinition = {

    def plainTextResponse(res: ResponseDefinitionBuilder): ResponseDefinitionBuilder = {

      val requestUrl = request.getUrlStripSlashes

      requestUrl match {

        case str if str == notFoundEmptyPlainTextBodyResponse =>
          res.withStatus(404)

        case str if str == notFoundPlainTextResponse =>
          res.withStatus(404).withBodyFile(requestUrl)

        case str if str == okEmptyPlainTextResponse =>
          res.withStatus(200)

        case _ => res.withStatus(200).withBodyFile(requestUrl)
      }
    }

    plainTextResponse(response.setContentType(ContentType.TEXT_PLAIN)).build()
  }

  override def getName: String = "resource-plaintext-transformer"
}
