package fsclient.config

case class AppConsumer(appName: String,
                       appVersion: Option[String],
                       appUrl: Option[String],
                       key: String,
                       secret: String) {

  private val version = appVersion.map(version => s"/$version").getOrElse("")
  private val url = appUrl.map(url => s" (+$url)").getOrElse("")

  // "name/version +(url)" (https://tools.ietf.org/html/rfc1945#section-3.7)
  lazy val userAgent: String = s"$appName$version$url"
}
