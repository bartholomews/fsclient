package io.bartholomews.fsclient.mocks.server

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import io.bartholomews.fsclient.mocks.MockEndpoints
import org.apache.http.entity.ContentType
import WiremockUtils._

case object ResourcePlainTextTransformer extends ResponseDefinitionTransformer with MockEndpoints {

  override val applyGlobally = false

  override def transform(
    request: Request,
    response: ResponseDefinition,
    files: FileSource,
    parameters: Parameters
  ): ResponseDefinition = {

    def plainTextResponse(res: ResponseDefinitionBuilder): ResponseDefinitionBuilder = {

      val requestUrl = request.getUrlStripSlashes

      requestUrl match {

        case str if str == notFoundEmptyPlainTextBodyResponse =>
          res
            .withHeader(`Content-Type`, ContentType.TEXT_PLAIN.getMimeType)
            .withStatus(404)

        case str if str == notFoundPlainTextResponse =>
          res
            .withHeader(`Content-Type`, ContentType.TEXT_PLAIN.getMimeType)
            .withStatus(404)
            .withBodyFile(requestUrl)

        case str if str == badRequestNoContentTypePlainTextResponse =>
          res.withStatus(400)

        case str if str == badRequestWrongContentTypePlainTextResponse =>
          res
            .withHeader(`Content-Type`, ContentType.MULTIPART_FORM_DATA.getMimeType)
            .withStatus(400)

        case str if str == okEmptyPlainTextResponse =>
          res
            .withHeader(`Content-Type`, ContentType.TEXT_PLAIN.getMimeType)
            .withStatus(200)

        case _ =>
          res
            .withHeader(`Content-Type`, ContentType.TEXT_PLAIN.getMimeType)
            .withStatus(200)
            .withBodyFile(requestUrl)
      }
    }

    plainTextResponse(ResponseDefinitionBuilder.like(response)).build()
  }

  override def getName: String = "resource-plaintext-transformer"
}
