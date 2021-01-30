import Dependencies.{circeDependencies, coreDependencies, playDependencies, testDependencies}
import sbt.Keys.{parallelExecution, scalacOptions}
import scoverage.ScoverageKeys.coverageFailOnMinimum

ThisBuild / scalaVersion := "2.13.3"

inThisBuild(
  List(
    organization := "io.bartholomews",
    homepage := Some(url("https://github.com/bartholomews/fsclient")),
    licenses := List("Unlicense" -> url("https://unlicense.org")),
    developers := List(
      Developer(
        "bartholomews",
        "Federico Bartolomei",
        "fsclient@bartholomews.io",
        url("https://bartholomews.io")
      )
    )
  )
)

val commonSettings = Seq(
  scalacOptions ++= Compiler.tpolecatOptions,
  scalacOptions ++= Seq(Compiler.unchecked, Compiler.deprecation),
  // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
  logBuffered in Test := false,
  parallelExecution in Test := false,
  testOptions in Test ++= TestSettings.options
)

lazy val core = (project in file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "fsclient-core",
    libraryDependencies ++= coreDependencies ++ testDependencies,
    coverageMinimum := 58, // FIXME
    coverageFailOnMinimum := true
  )

lazy val circe = (project in file("modules/circe"))
  .dependsOn(core % "test->test; compile->compile")
  .settings(commonSettings)
  .settings(
    name := "fsclient-circe",
    libraryDependencies ++= circeDependencies ++ testDependencies,
    coverageMinimum := 92, // FIXME
    coverageFailOnMinimum := true
  )

lazy val play = (project in file("modules/play"))
  .dependsOn(core % "test->test; compile->compile")
  .settings(commonSettings)
  .settings(
    name := "fsclient-play",
    libraryDependencies ++= playDependencies ++ testDependencies,
    coverageMinimum := 83, // FIXME
    coverageFailOnMinimum := true
  )

// https://www.scala-sbt.org/1.x/docs/Multi-Project.html
lazy val fsclient = (project in file("."))
  .settings(commonSettings)
  .settings(addCommandAlias("test", ";core/test;circe/test;play/test"): _*)
  .settings(skip in publish := true)
  .aggregate(core, circe, play)

resolvers += "Sonatype OSS Snapshots"
  .at("https://oss.sonatype.org/content/repositories/snapshots")

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")
