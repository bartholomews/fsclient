import Dependencies.{circeDependencies, coreDependencies, exclusions}
import sbt.Keys.{parallelExecution, scalacOptions}
import scoverage.ScoverageKeys.coverageFailOnMinimum

// https://github.com/scala/scala
ThisBuild / scalaVersion := "3.7.1"
// https://github.com/scala/scala3
ThisBuild / crossScalaVersions := Seq("2.13.16")

inThisBuild(
  List(
    organization := "io.bartholomews",
    homepage     := Some(url("https://github.com/bartholomews/fsclient")),
    licenses     := List("Unlicense" -> url("https://unlicense.org")),
    developers   := List(
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
  Test / logBuffered       := false,
  Test / parallelExecution := false,
  Test / testOptions ++= TestSettings.options
)

val compilerSettings = Seq(
  scalacOptions ++= Seq(
    "-encoding",
    "utf-8",                         // Specify character encoding used by source files.
    "-explaintypes",                 // Explain type errors in more detail.
    "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",        // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros", // Allow macro definition (besides implementation and application)
    "-language:higherKinds",         // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-unchecked",                    // Enable additional warnings where generated code depends on assumptions.
    "-Xfatal-warnings",              // Fail the compilation if there are any warnings.
    "-deprecation",
    "-Yretain-trees" // Enumeratum `findValues`
  )
)

val commonSettings = compilerSettings ++ testSettings

lazy val core = (project in file("modules/core"))
  .settings(commonSettings)
  .settings(
    name := "fsclient-core",
    libraryDependencies ++= coreDependencies, // ++ testDependencies.map(_ % Test),
    coverageMinimumStmtTotal := 58, // FIXME
    coverageFailOnMinimum    := true
  )

lazy val circe = (project in file("modules/circe"))
  .dependsOn(core % "compile->compile; test->test")
  .settings(commonSettings)
  .settings(
    name := "fsclient-circe",
    libraryDependencies ++= circeDependencies, // ++ testDependencies.map(_ % Test),
    excludeDependencies ++= exclusions,
    coverageMinimumStmtTotal := 65, // FIXME
    coverageFailOnMinimum    := true
  )

//lazy val testudo = (project in file("modules/testudo"))
//  .dependsOn(core % "compile->compile,test")
//  .settings(compilerSettings)
//  .settings(
//    name := "scalatestudo",
//    libraryDependencies ++= testDependencies
//  )

// https://www.scala-sbt.org/1.x/docs/Multi-Project.html
lazy val fsclient = (project in file("."))
  .settings(commonSettings)
  .settings(addCommandAlias("test", ";core/test;circe/test;play/test") *)
  .settings(publish / skip := true)
  .aggregate(core, circe) //, testudo)

resolvers += "Sonatype OSS Snapshots"
  .at("https://oss.sonatype.org/content/repositories/snapshots")

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")

ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
