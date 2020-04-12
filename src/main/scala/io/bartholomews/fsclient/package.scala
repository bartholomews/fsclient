package io.bartholomews

import io.circe.generic.extras.Configuration

package object fsclient {
  implicit val defaultConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
}
