package io.bartholomews.fsclient.mocks.server

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.http.{Request, ResponseDefinition}
import org.apache.http.entity.ContentType

object WiremockUtils {

  final val `Content-Type` = "Content-Type"

  implicit class RequestImplicits(request: Request) {
    implicit def getUrlStripSlashes: String =
      request.getUrl
        .stripPrefix("/")
        .stripSuffix("/")
  }

  implicit class ResponseDefinitionImplicits(response: ResponseDefinition) {
    implicit def error(code: Int, message: String): ResponseDefinition =
      ResponseDefinitionBuilder
        .like(response)
        .but()
        .withHeader(`Content-Type`, ContentType.TEXT_PLAIN.getMimeType)
        .withStatus(code)
        .withBody(message)
        .build()
  }

}
