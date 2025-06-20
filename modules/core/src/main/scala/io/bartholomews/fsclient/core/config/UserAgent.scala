package io.bartholomews.fsclient.core.config

import pureconfig.ConfigReader

final case class UserAgent(appName: String, appVersion: Option[String], appUrl: Option[String]) {

  private val version = appVersion.map(version => s"/$version").getOrElse("")
  private val url     = appUrl.map(url => s" (+$url)").getOrElse("")

  // "name/version +(url)" (https://tools.ietf.org/html/rfc1945#section-3.7)
  lazy val value: String = s"$appName$version$url"
}

object UserAgent {
  implicit val configReader: ConfigReader[UserAgent] =
    ConfigReader.forProduct3[UserAgent, String, Option[String], Option[String]](
      "app_name",
      "app_version",
      "app_url"
    )({ case (name, version, url) => UserAgent(name, version, url) })
}
