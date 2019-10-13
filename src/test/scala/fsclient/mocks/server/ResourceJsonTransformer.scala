package fsclient.mocks.server

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import fsclient.mocks.MockEndpoints
import WiremockUtils._
import org.apache.http.entity.ContentType

case object ResourceJsonTransformer extends ResponseDefinitionTransformer with MockEndpoints {

  override val applyGlobally = false

  override def transform(request: Request,
                         response: ResponseDefinition,
                         files: FileSource,
                         parameters: Parameters): ResponseDefinition = {

    def jsonResponse(res: ResponseDefinitionBuilder): ResponseDefinitionBuilder = {

      val requestUrl = request.getUrlStripSlashes

      requestUrl match {

        case str if str == notFoundEmptyJsonBodyResponse =>
          res
            .withHeader(`Content-Type`, ContentType.APPLICATION_JSON.getMimeType)
            .withStatus(404)

        case str if str == notFoundJsonResponse =>
          res
            .withHeader(`Content-Type`, ContentType.APPLICATION_JSON.getMimeType)
            .withStatus(404)
            .withBodyFile(s"$requestUrl.json")

        case str if str == badRequestNoContentTypeJsonResponse =>
          res.withStatus(400)

        case str if str == badRequestWrongContentTypeJsonResponse =>
          res
            .withHeader(`Content-Type`, ContentType.MULTIPART_FORM_DATA.getMimeType)
            .withStatus(400)

        case _ =>
          res
            .withHeader(`Content-Type`, ContentType.APPLICATION_JSON.getMimeType)
            .withStatus(200)
            .withBodyFile(s"$requestUrl.json")
      }
    }

    jsonResponse(ResponseDefinitionBuilder.like(response)).build()
  }

  override def getName: String = "resource-json-transformer"
}
