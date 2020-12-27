package io.bartholomews.fsclient.core.config

import cats.implicits.toBifunctorOps
import pureconfig.ConfigReader
import pureconfig.error.CannotConvert
import sttp.model.Uri

object implicits {
  implicit val sttpUriReader: ConfigReader[Uri] = ConfigReader[String].emap { strValue =>
    Uri
      .parse(strValue)
      .leftMap(error =>
        CannotConvert(
          value = strValue,
          toType = "sttp.model.Uri",
          because = error
        )
      )
  }
}
