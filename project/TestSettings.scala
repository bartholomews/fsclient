import sbt.{TestFrameworks, Tests}

object TestSettings {

  val options: Seq[Tests.Argument] = Seq(
    Tests.Argument(
      TestFrameworks.ScalaTest,
      "-oU", // enable standard output reporter
      "-u", // enable xml reporter
      "target/test-reports" // xml reporter output dir
    )
  )
}
