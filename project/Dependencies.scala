import sbt._

object Versions {
  // https://github.com/circe/circe-generic-extras/releases
  val circe_generic_extras = "0.14.1"
  // https://github.com/softwaremill/diffx
  val diffx = "0.4.0"
  // https://github.com/lloydmeta/enumeratum/releases
  val enumeratum = "1.7.0"
  // https://github.com/qos-ch/logback/releases
  val logback = "1.2.10"
  // https://github.com/pureconfig/pureconfig/releases
  val pureConfig = "0.17.1"
  // https://github.com/scalatest/scalatest/releases
  val scalaTest = "3.2.12"
  // https://github.com/softwaremill/sttp/releases
  val sttp = "3.3.18"
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

  // Dependencies of core which should be excluded by the other modules (which depends on core)
  lazy val exclusions = Seq(
    ExclusionRule(organization = "com.softwaremill.sttp.client3", name = "core"),
    ExclusionRule(organization = "com.beachape", name = "enumeratum")
  )

  lazy val testDependencies: Seq[ModuleID] = Seq(
    // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
    // https://www.scalactic.org
    "org.scalactic" %% "scalactic" % Versions.scalaTest,
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.softwaremill.diffx" %% "diffx-scalatest" % Versions.diffx,
    "com.github.tomakehurst" % "wiremock" % Versions.wiremock
  )
}
