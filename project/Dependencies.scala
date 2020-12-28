import sbt._

object Versions {
  // https://github.com/typelevel/cats/releases
  val cats = "2.2.0"
  // https://github.com/circe/circe/releases
  val circe = "0.14.0-M1"
  // https://github.com/circe/circe-generic-extras/releases
  val circe_generic_extras = "0.13.0"
  // https://github.com/qos-ch/logback/releases
  val logback = "1.2.3"
  // https://github.com/pureconfig/pureconfig/releases
  val pureConfig = "0.14.0"
  // https://github.com/scalatest/scalatest/releases
  val scalaTest = "3.2.3"
  // https://github.com/softwaremill/sttp/releases
  val sttp = "2.2.9"
  // https://github.com/tomakehurst/wiremock/releases
  val wiremock = "2.27.2"
}

object Dependencies {

  lazy val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic",
    "ch.qos.logback" % "logback-core"
  ).map(_ % Versions.logback)

  lazy val pureConfig: Seq[ModuleID] = Seq(
    "com.github.pureconfig" %% "pureconfig",
    "com.github.pureconfig" %% "pureconfig-cats-effect"
  ).map(_ % Versions.pureConfig)

  lazy val sttp = Seq(
    "com.softwaremill.sttp.client" %% "core" % Versions.sttp,
    "com.softwaremill.sttp.client" %% "circe" % Versions.sttp
  )

  lazy val typelevel: Seq[ModuleID] = Seq(
    "io.circe" %% "circe-generic-extras" % Versions.circe_generic_extras,
    // https://github.com/lloydmeta/enumeratum/releases
    "com.beachape" %% "enumeratum-circe" % "1.6.1"
  )

  val dependencies: Seq[ModuleID] =
    logback ++ pureConfig ++ sttp ++ typelevel

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % Versions.scalaTest,
    // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.github.tomakehurst" % "wiremock" % Versions.wiremock
  ).map(_ % Test)
}
