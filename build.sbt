import Dependencies.{dependencies, testDependencies}

organization := "io.bartholomews"
name := "fsclient"
scalaVersion := "2.13.1"
licenses += ("Unlicense", url("https://unlicense.org"))

version := "0.0.1-SNAPSHOT"

description := "Opinionated http wrapper on top of http4s."
homepage := Some(url("https://github.com/bartholomews/fsclient"))

scmInfo := Some(
  ScmInfo(url("https://github.com/bartholomews/fsclient"), "scm:git@github.com:bartholomews/fsclient.git")
)

organizationName := "io.bartholomews"
organizationHomepage := Some(url("https://bartholomews.io"))

publishMavenStyle := true

// Remove all additional repository other than Maven Central from POM
pomIncludeRepository := { _ =>
  false
}
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots".at(nexus + "content/repositories/snapshots"))
  else Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
}

credentials ++= Sonatype.credentials

developers := List(
  Developer(
    id = "bartholomews",
    name = "Federico Bartolomei",
    email = "fsclient@bartholomews.io",
    url = url("https://bartholomews.io")
  )
)

libraryDependencies ++= dependencies ++ testDependencies

scalacOptions ++= Compiler.tpolecatOptions
scalacOptions ++= Seq(Compiler.unchecked, Compiler.deprecation)

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

// http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")
