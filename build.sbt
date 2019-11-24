import Dependencies.{dependencies, testDependencies}

organization := "io.bartholomews"

name := "fsclient"

version := "0.2.1-SNAPSHOT"

skip in publish := isSnapshot.value

scalaVersion := "2.13.0"

licenses += ("Unlicense", url("https://unlicense.org"))

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
    "target/test-reports", // xml reporter output dir
  )
)

coverageEnabled := true
coverageMinimum := 78 // FIXME
coverageFailOnMinimum := true

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")
