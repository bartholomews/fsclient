package io.bartholomews.scalatestudo.wiremock

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.http.Request
import org.apache.http.entity.ContentType

trait WiremockUtils {

  implicit class RequestImplicits(request: Request) {
    implicit def getUrlStripSlashes: String =
      request.getUrl
        .stripPrefix("/")
        .stripSuffix("/")
  }

  implicit class ResponseDefinitionImplicits(res: ResponseDefinitionBuilder) {
    def withContentType(contentType: ContentType): ResponseDefinitionBuilder =
      res.withHeader("Content-Type", contentType.getMimeType)
  }

}

object WiremockUtils extends WiremockUtils {}
