import sbt._

object Versions {
  // https://github.com/typelevel/cats/releases
  val cats = "2.1.1"
  // https://github.com/typelevel/cats-effect/releases
  val cats_effect = "2.2.0"
  // https://github.com/circe/circe/releases
  val circe = "0.13.0"
  // https://github.com/circe/circe-fs2/releases
  val circe_fs2 = "0.13.0"
  // https://github.com/circe/circe-magnolia/releases
  val circe_magnolia = "0.6.1"
  // https://github.com/http4s/http4s/releases
  val http4s = "0.21.8"
  // https://github.com/lightbend/config/releases
  val lightbendConfig = "1.4.0"
  // https://github.com/qos-ch/logback/releases
  val logback = "1.2.3"
  // https://github.com/pureconfig/pureconfig/releases
  val pureConfig = "0.14.0"
  // https://github.com/scalatest/scalatest/releases
  val scalaTest = "3.2.2"
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

  lazy val typelevel: Seq[ModuleID] = Seq(
    "com.typesafe" % "config" % Versions.lightbendConfig,
    "org.typelevel" %% "cats-effect" % Versions.cats_effect,
    "org.http4s" %% "http4s-dsl" % Versions.http4s,
    "org.http4s" %% "http4s-blaze-client" % Versions.http4s,
    "org.http4s" %% "http4s-circe" % Versions.http4s,
    "io.circe" %% "circe-fs2" % Versions.circe_fs2,
    "io.circe" %% "circe-generic-extras" % Versions.circe,
    // string interpolation to JSON model
    "io.circe" %% "circe-literal" % Versions.circe,
    // https://github.com/lloydmeta/enumeratum/releases
    "com.beachape" %% "enumeratum-circe" % "1.6.1"
  )

  lazy val apache: Seq[ModuleID] = Seq(
    // https://mvnrepository.com/artifact/org.apache.httpcomponents/httpcore
    "org.apache.httpcomponents" % "httpcore" % "4.4.13"
  )

  val dependencies: Seq[ModuleID] =
    apache ++ logback ++ pureConfig ++ typelevel

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "org.scalactic" %% "scalactic" % Versions.scalaTest,
    // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
    "org.scalatest" %% "scalatest" % Versions.scalaTest,
    "com.github.tomakehurst" % "wiremock" % Versions.wiremock
  ).map(_ % Test)
}
