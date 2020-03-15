import Dependencies.{dependencies, testDependencies}

organization := "io.bartholomews"
name := "fsclient"
scalaVersion := "2.13.1"
licenses += ("Unlicense", url("https://unlicense.org"))

ThisBuild / organizationName := "io.bartholomews"
ThisBuild / organizationHomepage := Some(url("https://bartholomews.io"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/bartholomews/fsclient"),
    "scm:git@github.com:bartholomews/fsclient.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "bartholomews",
    name  = "Federico Bartolomei",
    email = "fsclient@bartholomews.io",
    url   = url("https://bartholomews.io")
  )
)

ThisBuild / description := "Opinionated http wrapper on top of http4s."
ThisBuild / homepage := Some(url("https://github.com/bartholomews/fsclient"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

version := "0.0.1-SNAPSHOT"
publishMavenStyle := true

credentials ++= Sonatype.credentials

libraryDependencies ++= dependencies ++ testDependencies

scalacOptions ++= Compiler.tpolecatOptions
scalacOptions ++= Seq(Compiler.unchecked, Compiler.deprecation)

// http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

testOptions in Test ++= Seq(
  Tests.Argument(
    TestFrameworks.ScalaTest,
    "-oU", // enable standard output reporter
    "-u", // enable xml reporter
    "target/test-reports" // xml reporter output dir
  )
)

coverageMinimum := 56 // FIXME
coverageFailOnMinimum := true

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")
