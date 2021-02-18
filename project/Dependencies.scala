import sbt._

object Versions {
  // https://github.com/circe/circe-generic-extras/releases
  val circe_generic_extras = "0.13.0"
  // https://github.com/lloydmeta/enumeratum/releases
  val enumeratum = "1.6.1"
  // https://github.com/qos-ch/logback/releases
  val logback = "1.2.3"
  // https://github.com/pureconfig/pureconfig/releases
  val pureConfig = "0.14.0"
  // https://github.com/scalatest/scalatest/releases
  val scalaTest = "3.2.4"
  // https://github.com/softwaremill/sttp/releases
  val sttp = "3.1.2"
  // https://github.com/tomakehurst/wiremock/releases
  val wiremock = "2.27.2"
}

object Dependencies {

  lazy val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic",
    "ch.qos.logback" % "logback-core"
  ).map(_ % Versions.logback)

  lazy val coreDependencies: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig,
    "com.softwaremill.sttp.client3" %% "core" % Versions.sttp,
    "com.beachape" %% "enumeratum" % Versions.enumeratum
  ) ++ logback

  lazy val circeDependencies: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic-extras" % Versions.circe_generic_extras,
    "com.softwaremill.sttp.client3" %% "circe" % Versions.sttp,
    "com.beachape" %% "enumeratum-circe" % Versions.enumeratum
  )

  lazy val playDependencies: Seq[ModuleID] = Seq(
    "com.softwaremill.sttp.client3" %% "play-json" % Versions.sttp,
    "com.beachape" %% "enumeratum-play-json" % Versions.enumeratum
  )

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % Versions.scalaTest,
    // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.github.tomakehurst" % "wiremock" % Versions.wiremock
  ).map(_ % Test)
}
