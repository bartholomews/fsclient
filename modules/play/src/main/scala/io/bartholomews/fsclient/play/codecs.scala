package io.bartholomews.fsclient.play

import play.api.libs.json.{Json, JsonConfiguration, JsonNaming}

object codecs extends FsClientPlayApi {
  val withDiscriminator: Json.WithOptions[Json.MacroOptions] =
    Json.configured(
      JsonConfiguration(
        discriminator = "type",
        typeNaming = JsonNaming { fullyQualifiedName =>
          val fr: String = fullyQualifiedName.split("\\.").last
          val tail = fr.tail.foldLeft("") { (acc, curr) =>
            if (curr.isUpper) s"${acc}_${curr.toLower}" else s"$acc$curr"
          }
          s"${fr.head.toLower}$tail"
        }
      )
    )
}
