package com.thetestpeople.trt.model.impl

import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import com.thetestpeople.trt.utils.UriUtils._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import org.joda.time.Duration
import java.net.URI

trait ExecutionDaoTest { self: AbstractDaoTest ⇒

  "Inserting a new execution" should "persist all the execution data" in transaction { dao ⇒
    val testName = QualifiedName(DummyData.TestName, Some(DummyData.Group))
    val testId = dao.ensureTestIsRecorded(F.test(testName))
    val batchName = DummyData.BatchName
    val batchId = dao.newBatch(F.batch(nameOpt = Some(batchName)))

    val executionId = dao.newExecution(F.execution(
      batchId = batchId,
      testId = testId,
      executionTime = DummyData.ExecutionTime,
      durationOpt = Some(DummyData.Duration),
      summaryOpt = Some(DummyData.Summary),
      passed = true),
      logOpt = Some(DummyData.Log))

    val Some(execution) = dao.getEnrichedExecution(executionId)
    execution.batchId should equal(batchId)
    execution.testId should equal(testId)
    execution.executionTime should equal(DummyData.ExecutionTime)
    execution.durationOpt should equal(Some(DummyData.Duration))
    execution.summaryOpt should equal(Some(DummyData.Summary))
    execution.passed should be(true)
    execution.qualifiedName should equal(testName)
    execution.batchNameOpt should equal(Some(batchName))
    execution.logOpt should equal(Some(DummyData.Log))

    val Some(log) = dao.getExecutionLog(executionId)
    log should equal(DummyData.Log)
  }

  "Inserting a new execution" should "handle absent data" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    val executionId = dao.newExecution(F.execution(
      batchId = batchId,
      testId = testId,
      durationOpt = None,
      summaryOpt = None),
      None)

    val Some(execution) = dao.getEnrichedExecution(executionId)
    execution.durationOpt should equal(None)
    execution.summaryOpt should equal(None)
    execution.logOpt should equal(None)
  }

  "Iterating all executions" should "return them sorted by configuration, test id, and execution time" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    val testId2 = dao.ensureTestIsRecorded(F.test())
    val testId3 = dao.ensureTestIsRecorded(F.test())
    val executionsIn = List(
      ExecutionLite(Configuration("aadvarkConfiguration"), testId1, executionTime = 1.day.ago, passed = true),
      ExecutionLite(Configuration("aadvarkConfiguration"), testId2, executionTime = 1.day.ago, passed = false),
      ExecutionLite(Configuration("aadvarkConfiguration"), testId3, executionTime = 1.day.ago, passed = true),
      ExecutionLite(Configuration("zebraConfiguration"), testId1, executionTime = 2.days.ago, passed = false),
      ExecutionLite(Configuration("zebraConfiguration"), testId1, executionTime = 1.day.ago, passed = true))
    for (execution ← executionsIn) {
      val batchId = dao.newBatch(F.batch())
      dao.newExecution(F.execution(batchId, execution.testId,
        configuration = execution.configuration,
        executionTime = execution.executionTime, passed = execution.passed))
    }

    val executionsOut = dao.iterateAllExecutions(_.toList)

    executionsOut should equal(executionsIn)
  }

  "Iterating all executions" should "exclude deleted tests" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    val testId2 = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId, testId1))
    dao.newExecution(F.execution(batchId, testId1))
    dao.newExecution(F.execution(batchId, testId2))
    dao.markTestsAsDeleted(Seq(testId1))

    val executions = dao.iterateAllExecutions(_.toList)

    executions.map(_.testId) should equal(Seq(testId2))
  }

  "Getting executions for a test" should "return them most recent first" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId1 = dao.newBatch(F.batch())
    val batchId2 = dao.newBatch(F.batch())
    val executionId1 = dao.newExecution(F.execution(batchId1, testId, executionTime = 1.day.ago))
    val executionId3 = dao.newExecution(F.execution(batchId2, testId, executionTime = 3.days.ago))
    val executionId2 = dao.newExecution(F.execution(batchId2, testId, executionTime = 2.days.ago))

    val executions = dao.getExecutionsForTest(testId)

    executions.map(_.id) should equal(List(executionId1, executionId2, executionId3))
  }

  "Getting test executions in a batch" should "return the executions" in transaction { dao ⇒
    val test1 = F.test()
    val test2 = F.test()
    val test3 = F.test()
    val testId1 = dao.ensureTestIsRecorded(test1)
    val testId2 = dao.ensureTestIsRecorded(test2)
    val testId3 = dao.ensureTestIsRecorded(test3)
    val batchId = dao.newBatch(F.batch())
    val executionId1 = dao.newExecution(F.execution(batchId, testId1))
    val executionId2 = dao.newExecution(F.execution(batchId, testId2))
    val executionId3 = dao.newExecution(F.execution(batchId, testId3))

    val executions = dao.getEnrichedExecutionsInBatch(batchId)

    executions.map(_.execution.id) should contain theSameElementsAs List(executionId1, executionId2, executionId3)
    executions.map(_.qualifiedName) should contain theSameElementsAs List(test1.qualifiedName, test2.qualifiedName, test3.qualifiedName)
  }

  it should "let you filter by pass/fail" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    val testId2 = dao.ensureTestIsRecorded(F.test())
    val testId3 = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    val executionId1 = dao.newExecution(F.execution(batchId, testId1, passed = true))
    val executionId2 = dao.newExecution(F.execution(batchId, testId2, passed = true))
    val executionId3 = dao.newExecution(F.execution(batchId, testId3, passed = false))

    val passedExecutions = dao.getEnrichedExecutionsInBatch(batchId, passedFilterOpt = Some(true))
    passedExecutions.map(_.execution.id) should contain theSameElementsAs List(executionId1, executionId2)

    val failedExecutions = dao.getEnrichedExecutionsInBatch(batchId, passedFilterOpt = Some(false))
    failedExecutions.map(_.execution.id) should equal(List(executionId3))
  }

  "Counting executions" should "let you count all records" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    val count = 10
    for (n ← 1 to count)
      dao.newExecution(F.execution(batchId, testId))
    dao.countExecutions() should equal(count)
  }

  it should "let you restrict results to a specific configuration" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration1))
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration1))
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration2))

    dao.countExecutions(configurationOpt = Some(DummyData.Configuration1)) should equal(2)
    dao.countExecutions(configurationOpt = Some(DummyData.Configuration2)) should equal(1)
  }

  "Getting execution intervals by configuration" should "work" in transaction { dao ⇒
    def addExecution(configuration: Configuration, executionTime: DateTime) {
      val testId = dao.ensureTestIsRecorded(F.test())
      val batchId = dao.newBatch(F.batch())
      dao.newExecution(F.execution(batchId, testId, configuration = configuration, executionTime = executionTime))
    }
    val earliestTime1 = 10.days.ago
    val earliestTime2 = 20.days.ago
    val latestTime1 = 1.day.ago
    val latestTime2 = 2.days.ago
    addExecution(DummyData.Configuration1, earliestTime1)
    addExecution(DummyData.Configuration1, latestTime1)
    addExecution(DummyData.Configuration2, earliestTime2)
    addExecution(DummyData.Configuration2, latestTime2)

    dao.getExecutionIntervalsByConfig() should equal(Map(
      DummyData.Configuration1 -> new Interval(earliestTime1, latestTime1),
      DummyData.Configuration2 -> new Interval(earliestTime2, latestTime2)))
  }

  "Getting enriched executions" should "return results most recent first" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    def addExecution(executionTime: DateTime) = dao.newExecution(F.execution(batchId, testId, executionTime = executionTime))
    val executionId1 = addExecution(1.day.ago)
    val executionId3 = addExecution(3.days.ago)
    val executionId2 = addExecution(2.days.ago)

    val executions = dao.getEnrichedExecutions()

    executions.map(_.id) should equal(List(executionId1, executionId2, executionId3))
  }

  // Would like to do this to make execution retrieval order more stable, but it impacts SQL performance
  it should "sort by execution time, then test group, then test name" ignore transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test(name = "Alice", groupOpt = Some("Aardvark")))
    val testId2 = dao.ensureTestIsRecorded(F.test(name = "Bob", groupOpt = Some("Aardvark")))
    val testId3 = dao.ensureTestIsRecorded(F.test(name = "Charlie", groupOpt = Some("Beard")))
    val batchId = dao.newBatch(F.batch())
    val executionId2 = dao.newExecution(F.execution(batchId, testId2))
    val executionId1 = dao.newExecution(F.execution(batchId, testId1))
    val executionId3 = dao.newExecution(F.execution(batchId, testId3))

    val executions = dao.getEnrichedExecutions()

    executions.map(_.id) should equal(List(executionId1, executionId2, executionId3))
  }

  it should "allow filtering by configuration" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    def addExecution(configuration: Configuration) = dao.newExecution(F.execution(batchId, testId, configuration = configuration))
    val executionId1 = addExecution(DummyData.Configuration1)
    val executionId2 = addExecution(DummyData.Configuration2)

    val executions = dao.getEnrichedExecutions(configurationOpt = Some(DummyData.Configuration1))

    executions.map(_.id) should equal(List(executionId1))
  }

  it should "allow pagination" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    def addExecution(executionTime: DateTime) = dao.newExecution(F.execution(batchId, testId, executionTime = executionTime))
    val executionId1 = addExecution(1.day.ago)
    val executionId2 = addExecution(2.days.ago)
    val executionId3 = addExecution(3.days.ago)
    val executionId4 = addExecution(4.days.ago)
    val executionId5 = addExecution(5.days.ago)
    val executionId6 = addExecution(6.days.ago)

    dao.getEnrichedExecutions(startingFrom = 0, limit = 3).map(_.id) should equal(List(executionId1, executionId2, executionId3))
    dao.getEnrichedExecutions(startingFrom = 3, limit = 3).map(_.id) should equal(List(executionId4, executionId5, executionId6))
  }

  "Getting enriched executions for a test" should "return results most recent first" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    def addExecution(executionTime: DateTime) = dao.newExecution(F.execution(batchId, testId, executionTime = executionTime))
    val executionId1 = addExecution(1.day.ago)
    val executionId3 = addExecution(3.days.ago)
    val executionId2 = addExecution(2.days.ago)

    val executions = dao.getEnrichedExecutionsForTest(testId)

    executions.map(_.id) should equal(List(executionId1, executionId2, executionId3))
  }

  it should "allow filtering by configuration" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    def addExecution(configuration: Configuration) = dao.newExecution(F.execution(batchId, testId, configuration = configuration))
    val executionId1 = addExecution(DummyData.Configuration1)
    val executionId2 = addExecution(DummyData.Configuration2)

    val executions = dao.getEnrichedExecutionsForTest(testId, configurationOpt = Some(DummyData.Configuration1))

    executions.map(_.id) should equal(List(executionId1))
  }

  "Getting enriched executions" should "work for a list of IDs" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    def addExecution() = dao.newExecution(F.execution(batchId, testId))
    val executionId1 = addExecution()
    val executionId2 = addExecution()
    val executionId3 = addExecution()

    val executions = dao.getEnrichedExecutions(Seq(executionId1, executionId2))

    executions.map(_.id) should contain theSameElementsAs (Seq(executionId1, executionId2))
  }

}