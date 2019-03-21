package reporter

import org.scalatest._
import org.scalatest.events.{Event, SuiteCompleted, TestStarting}

case class CustomReporter() extends Reporter with ResourcefulReporter {

  override def dispose: Unit = {
    println("?" * 50)
    println(TagsTracker.get)
    println("?" * 50)
  }

  override def apply(event: Event): Unit = {

    event match {

      case TestStarting(ordinal, suiteName, suiteId, suiteClassName, testName, testText, formatter, location, rerunner, payload, threadName, timeStamp) =>
        println("x" * 50)
        println("TEST STARTING")
        println(ordinal)
        println(suiteName)
        println(suiteId)
        println(suiteClassName)
        println(testName)
        println(testText)
        println(formatter)
        println(location)
        println(rerunner)
        println(payload)
        println(threadName)
        println(timeStamp)
        println("x" * 50)

      case SuiteCompleted(_, _, _, _, _, _, _, _, _, _, _) =>
        println("~" * 50)
        println(TagsTracker.allTags)
        println("~" * 50)

      case _ => ()
    }
  }

}
