import Dependencies.{dependencies, testDependencies}

name := "fsclient"
scalaVersion := "2.13.2"

inThisBuild(List(
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
))

resolvers += "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
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
