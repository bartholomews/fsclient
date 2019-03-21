package reporter

import org.scalatest.{BeforeAndAfterEachTestData, Suite, TestData}

import scala.collection.mutable

trait TagsTracker extends BeforeAndAfterEachTestData {

  this: Suite =>

  override def beforeEach(testData: TestData): Unit = {
    TagsTracker.allTags.++=(testData.tags)
    println("~" * 50)
    println(TagsTracker.allTags)
    println("~" * 50)
    super.beforeEach(testData)
  }
}

object TagsTracker {
  val allTags = new mutable.SetBuilder[String, Set[String]](Set.empty) // new mutable.HashSet[String]
  def get: Set[String] = allTags.result()
}
