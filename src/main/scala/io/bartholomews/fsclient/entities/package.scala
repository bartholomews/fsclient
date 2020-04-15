package io.bartholomews.fsclient

import io.circe.generic.extras.Configuration

package object entities {
  implicit val defaultConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
