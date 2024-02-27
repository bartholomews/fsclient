import Dependencies.{circeDependencies, coreDependencies, exclusions, playDependencies, testDependencies}
import sbt.Keys.{parallelExecution, scalacOptions}
import scoverage.ScoverageKeys.coverageFailOnMinimum

// https://github.com/scala/scala
ThisBuild / scalaVersion := "2.13.13"

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

val testSettings = Seq(
  // http://www.scalatest.org/user_guide/using_scalatest_with_sbt
  Test / logBuffered := false,
  Test / parallelExecution := false,
  Test / testOptions ++= TestSettings.options
)

val compilerSettings = Seq(
  scalacOptions ++= Compiler.tpolecatOptions ++ Seq(Compiler.unchecked, Compiler.deprecation)
)

val commonSettings = compilerSettings ++ testSettings

lazy val core = (project in file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "fsclient-core",
    libraryDependencies ++= coreDependencies ++ testDependencies.map(_ % Test),
    coverageMinimum := 58, // FIXME
    coverageFailOnMinimum := true
  )

lazy val circe = (project in file("modules/circe"))
  .dependsOn(core % "compile->compile; test->test")
  .settings(commonSettings)
  .settings(
    name := "fsclient-circe",
    libraryDependencies ++= circeDependencies ++ testDependencies.map(_ % Test),
    excludeDependencies ++= exclusions,
    coverageMinimum := 65, // FIXME
    coverageFailOnMinimum := true
  )

lazy val play = (project in file("modules/play"))
  .dependsOn(core % "compile->compile; test->test")
  .settings(commonSettings)
  .settings(
    name := "fsclient-play",
    libraryDependencies ++= playDependencies ++ testDependencies.map(_ % Test),
    excludeDependencies ++= exclusions,
    coverageMinimum := 69, // FIXME
    coverageFailOnMinimum := true
  )


lazy val testudo = (project in file("modules/testudo"))
  .dependsOn(core % "compile->compile,test")
  .settings(compilerSettings)
  .settings(
    name := "scalatestudo",
    libraryDependencies ++= testDependencies
  )

// https://www.scala-sbt.org/1.x/docs/Multi-Project.html
lazy val fsclient = (project in file("."))
  .settings(commonSettings)
  .settings(addCommandAlias("test", ";core/test;circe/test;play/test"): _*)
  .settings(publish / skip := true)
  .aggregate(core, circe, play, testudo)

resolvers += "Sonatype OSS Snapshots"
  .at("https://oss.sonatype.org/content/repositories/snapshots")

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")
