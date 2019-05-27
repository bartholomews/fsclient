import sbt._

object Versions {
  val cats = "2.0.0-M2"
  val cats_effect = "2.0.0-M2"
  val circe = "0.12.0-M1"
  val circe_fs2 = "0.11.0"
  val circe_magnolia = "0.4.0"
  val http4s = "0.20.1"
  val lightbendConfig = "1.3.4"
  val logback = "1.3.0-alpha4"
  val pureConfig = "0.11.0"
  val scalaTest = "3.0.7"
  val wiremock = "2.23.2"
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

  lazy val typelevel: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % Versions.lightbendConfig,
    "org.typelevel" %% "cats-effect" % Versions.cats_effect,
    "org.http4s" %% "http4s-dsl" % Versions.http4s,
    "org.http4s" %% "http4s-blaze-client" % Versions.http4s,
    "org.http4s" %% "http4s-circe" % Versions.http4s,
    "io.circe" %% "circe-fs2" % Versions.circe_fs2,
    "io.circe" %% "circe-generic-extras" % Versions.circe,
    "io.circe" %% "circe-literal" % Versions.circe // string interpolation to JSON model
  )

  val dependencies: Seq[ModuleID] =
    logback ++ pureConfig ++ typelevel

  lazy val testDependencies: Seq[ModuleID] = Seq(
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-log4j12
    // libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.25" % Test
    "org.scalactic" %% "scalactic" % Versions.scalaTest,
    // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.github.tomakehurst" % "wiremock" % Versions.wiremock,
    "org.pegdown" % "pegdown" % "1.6.0" // required for scalatest html report
  ).map(_ % Test)
}