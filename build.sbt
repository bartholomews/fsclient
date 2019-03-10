import Dependencies._

organization := "com.github.bartholomews"

name := "fsclient"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"

libraryDependencies ++= dependencies ++ testDependencies

scalacOptions ++= Compiler.options

coverageEnabled := true

// http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false
parallelExecution in ThisBuild := false