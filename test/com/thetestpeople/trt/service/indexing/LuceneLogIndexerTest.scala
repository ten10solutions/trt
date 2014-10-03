package com.thetestpeople.trt.service.indexing

import org.junit.runner.RunWith
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model._
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class LuceneLogIndexerTest extends FlatSpec with ShouldMatchers {

  "Searching before adding any documents" should "return empty results, and not blow up" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    indexer.searchExecutions("foo").hits should equal(Seq())
  }

  "Searching for a document by its exact log" should "return it" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution = makeExecution(log = "foo")
    indexer.addExecutions(Seq(execution))

    val SearchResult(Seq(ExecutionHit(executionId, fragment)), totalCount) = indexer.searchExecutions("foo")

    executionId should equal(execution.id)
    totalCount should equal(1)
    fragment should equal("<span class='search-result'>foo</span>")
  }

  "Searching for a document" should "return only those matching all the search keywords" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution1 = makeExecution(log = "foo bar baz")
    val execution2 = makeExecution(log = "foo baz quux")
    val execution3 = makeExecution(log = "boo bar baz")
    indexer.addExecutions(Seq(execution1, execution2, execution3))

    val Seq(hit) = indexer.searchExecutions("quux foo").hits

    hit.executionId should equal(execution2.id)
  }

  "Searching for documents" should "return results ordered most recent first" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution1 = makeExecution(log = "foo", executionTime = 1.day.ago)
    val execution2 = makeExecution(log = "foo", executionTime = 2.days.ago)
    val execution3 = makeExecution(log = "foo", executionTime = 3.days.ago)
    indexer.addExecutions(Seq(execution2, execution3, execution1))

    val executionIds = indexer.searchExecutions("foo").hits.map(_.executionId)

    executionIds should equal(Seq(execution1.id, execution2.id, execution3.id))
  }

  "Searching for documents" should "allow paging" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution0 = makeExecution(log = "foo", executionTime = 1.day.ago)
    val execution1 = makeExecution(log = "foo", executionTime = 2.days.ago)
    val execution2 = makeExecution(log = "foo", executionTime = 3.days.ago)
    val execution3 = makeExecution(log = "foo", executionTime = 4.days.ago)
    val execution4 = makeExecution(log = "foo", executionTime = 5.days.ago)
    val execution5 = makeExecution(log = "foo", executionTime = 6.days.ago)
    indexer.addExecutions(Seq(execution0, execution1, execution2, execution3, execution4, execution5))

    indexer.searchExecutions("foo", startingFrom = 2, limit = 3).hits.map(_.executionId) should equal(
      Seq(execution2.id, execution3.id, execution4.id))

    indexer.searchExecutions("foo", startingFrom = 0, limit = 10).hits.map(_.executionId) should equal(
      Seq(execution0.id, execution1.id, execution2.id, execution3.id, execution4.id, execution5.id))

    indexer.searchExecutions("foo", startingFrom = 5, limit = 4).hits.map(_.executionId) should equal(
      Seq(execution5.id))
  }

  "Deleting all documents" should "work" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution = makeExecution(log = "foo")
    indexer.addExecutions(Seq(execution))

    indexer.deleteAll()

    indexer.searchExecutions("foo").hits should equal(Seq())
  }

  "Deleting executions" should "work too" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution1 = makeExecution(log = "foo")
    val execution2 = makeExecution(log = "foo")
    val execution3 = makeExecution(log = "foo")
    indexer.addExecutions(Seq(execution1, execution2, execution3))

    indexer.deleteExecutions(Seq(execution1.id, execution2.id))

    val Seq(hit) = indexer.searchExecutions("foo").hits
    hit.executionId should equal(execution3.id)
  }

  "Searching for parts of a dotted identifier" should "succeed" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution = makeExecution(log = "java.lang.NullPointerException")
    indexer.addExecutions(Seq(execution))

    indexer.searchExecutions("java.lang.NullPointerException").total should equal(1)
    indexer.searchExecutions("NullPointerException").total should equal(1)

  }

  "Searching for numbers" should "succeed" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution = makeExecution(log = "10 12345 20")
    indexer.addExecutions(Seq(execution))

    indexer.searchExecutions("12345").total should equal(1)
  }

  "Searching for tokens containing numbers" should "succeed" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution = makeExecution(log = "foo bar42 baz")
    indexer.addExecutions(Seq(execution))

    indexer.searchExecutions("bar42").total should equal(1)
  }

  "Search" should "be case insensitive" in {
    val indexer = LuceneLogIndexer.memoryBackedIndexer
    val execution = makeExecution(log = "FOO bar")
    indexer.addExecutions(Seq(execution))

    indexer.searchExecutions("foo").total should equal(1)
    indexer.searchExecutions("BAR").total should equal(1)
  }

  private var executionIdCounter = 0

  private def makeExecution(log: String, executionTime: DateTime = DummyData.ExecutionTime): EnrichedExecution = {
    val execution = F.execution(Id.dummy, Id.dummy, executionTime = executionTime).copy(id = Id[Execution](executionIdCounter))
    executionIdCounter += 1
    EnrichedExecution(execution, DummyData.QualifiedTestName, batchNameOpt = None, logOpt = Some(log))
  }

}
