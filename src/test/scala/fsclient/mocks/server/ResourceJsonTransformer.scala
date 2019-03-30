package fsclient.mocks.server

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.{Parameters, ResponseDefinitionTransformer}
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import fsclient.mocks.MockEndpoints

case object ResourceJsonTransformer extends ResponseDefinitionTransformer with MockEndpoints {

  override val applyGlobally = false

  override def transform(request: Request,
                         response: ResponseDefinition,
                         files: FileSource,
                         parameters: Parameters): ResponseDefinition = {

    val requestUrl: String = {
      val url = request.getUrl
      if (url.startsWith("/")) url.drop(1) else url
    }

    def jsonResponse(res: ResponseDefinitionBuilder): ResponseDefinitionBuilder = {
      requestUrl match {

        case str if str == notFoundEmptyJsonBodyResponse =>
          res.withStatus(404)

        case str if str == notFoundJsonResponse =>
          res.withStatus(404).withBodyFile(s"$requestUrl.json")

        case _ => res.withStatus(200).withBodyFile(s"$requestUrl.json")
      }
    }

    val builder = ResponseDefinitionBuilder.like(response)
      .but()
      .withHeader("Content-Type", "application/json")

    jsonResponse(builder).build()
  }

  override def getName: String = "resource-json-transformer"
}
