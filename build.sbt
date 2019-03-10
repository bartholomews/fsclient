import Dependencies.{dependencies, testDependencies}

organization := "org.github.bartholomews"

name := "fsclient"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"

licenses += ("Unlicense", url("https://unlicense.org"))

libraryDependencies ++= dependencies ++ testDependencies

scalacOptions ++= Compiler.options

coverageEnabled := true
coverageMinimum := 0
coverageFailOnMinimum := true

// http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false
parallelExecution in ThisBuild := false

skip in publish := isSnapshot.value
