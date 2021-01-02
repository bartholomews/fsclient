package io.bartholomews.fsclient.core

import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import sttp.model.Uri

package object config {
  implicit val sttpUriReader: ConfigReader[Uri] = ConfigReader[String].emap { strValue =>
    Uri
      .parse(strValue)
      .left
      .map(error =>
        CannotConvert(
          value = strValue,
          toType = "sttp.model.Uri",
          because = error
        )
      )
  }
}
