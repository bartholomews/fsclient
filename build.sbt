import Dependencies.{dependencies, testDependencies}

organization := "io.bartholomews"
name := "fsclient"
scalaVersion := "2.13.1"
licenses += ("Unlicense", url("https://unlicense.org"))

version := "0.0.1-SNAPSHOT"

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("bartholomews", "fsclient", "fsclient@bartholomews.io"))

publishTo := sonatypePublishToBundle.value
publishMavenStyle := true

libraryDependencies ++= dependencies ++ testDependencies

scalacOptions ++= Compiler.tpolecatOptions
scalacOptions ++= Seq(Compiler.unchecked, Compiler.deprecation)

testOptions in Test ++= TestSettings.options
coverageMinimum := 44.74 // FIXME
coverageFailOnMinimum := true

// http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

addCommandAlias("test-coverage", ";clean ;coverage ;test ;coverageReport")
addCommandAlias("test-fast", "testOnly * -- -l org.scalatest.tags.Slow")
