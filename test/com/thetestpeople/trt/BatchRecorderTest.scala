package com.thetestpeople.trt

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.MockDao
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.service._
import com.github.nscala_time.time.Imports._
import java.net.URI
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.model.impl.DummyData

@RunWith(classOf[JUnitRunner])
class BatchRecorderTest extends FlatSpec with Matchers {

  "Submitting a batch with a single test" should "correctly capture all input data" in {
    val (dao, batchRecorder, analysisService) = getTestContext()
    val inTest = F.test("testMethod", Some("TestClass"))
    val inExecution = F.execution(inTest,
      passed = true,
      summaryOpt = Some("Blah"),
      logOpt = Some("Blither"),
      executionTimeOpt = Some(new DateTime),
      durationOpt = Some(8.seconds),
      configurationOpt = Some(DummyData.Configuration1))
    val inBatch = F.batch(
      urlOpt = Some(uri("http://www.blah.com")),
      nameOpt = Some("Blahblah"),
      logOpt = Some("Blitherblither"),
      executionTimeOpt = Some(new DateTime),
      durationOpt = Some(8.seconds),
      executions = List(inExecution))

    val batch = batchRecorder.recordBatch(inBatch)

    val List(execution) = dao.getEnrichedExecutionsInBatch(batch.id)
    val List(TestAndAnalysis(test, _)) = dao.getAnalysedTests()
    checkBatchDataWasCaptured(batch, inBatch)
    checkExecutionDataWasCaptured(execution, inExecution)
    checkTestDataWasCaptured(test, inTest)
    execution.batchId should equal(batch.id)
    execution.testId should equal(test.id)
  }

  "Submitting a batch with multiple tests" should "capture all the input data" in {
    val (dao, batchRecorder, analysisService) = getTestContext()
    val inTest1 = F.test("test1")
    val inExecution1 = F.execution(inTest1)
    val inTest2 = F.test("test2")
    val inExecution2 = F.execution(inTest2)
    val inBatch = F.batch(executions = List(inExecution1, inExecution2))

    val batch = batchRecorder.recordBatch(inBatch)

    val _executions = dao.getEnrichedExecutionsInBatch(batch.id, passedFilterOpt = None)
    _executions.size should be(2)
    val execution1 = findExecution(dao, batch.id, inTest1.qualifiedName)
    val execution2 = findExecution(dao, batch.id, inTest2.qualifiedName)
    val Some(TestAndAnalysis(test1, _)) = dao.getTestAndAnalysis(execution1.testId)
    val Some(TestAndAnalysis(test2, _)) = dao.getTestAndAnalysis(execution2.testId)
    checkBatchDataWasCaptured(batch, inBatch)
    checkExecutionDataWasCaptured(execution1, inExecution1)
    checkExecutionDataWasCaptured(execution2, inExecution2)
    checkTestDataWasCaptured(test1, inTest1)
    checkTestDataWasCaptured(test2, inTest2)
  }

  "Submitting a batch with a single failing test" should "record the batch as failing" in {
    val inExecution = F.execution(F.test("testSomething"), passed = false)
    val inBatch = F.batch(executions = List(inExecution))
    val (_, batchRecorder, _) = getTestContext()

    val batch = batchRecorder.recordBatch(inBatch)

    batch.passed should be(false)
  }

  "Submitting a batch without a timestamp" should "be given the current time" in {
    val inExecution = F.execution(F.test("testSomething"), executionTimeOpt = None)
    val inBatch = F.batch(executions = List(inExecution), executionTimeOpt = None)
    val now = new DateTime
    val (dao, batchRecorder, _) = getTestContext(clock = FakeClock(now))

    val batch = batchRecorder.recordBatch(inBatch)
    batch.executionTime should equal(now)
    val List(execution) = dao.getEnrichedExecutionsInBatch(batch.id)
    execution.executionTime should equal(now)
  }

  "Submitting a batch without a configuration" should "have a configuration inferred from the executions if they are all the same" in {
    val inExecution1 = F.execution(F.test("test1"), configurationOpt = Some(DummyData.Configuration1))
    val inExecution2 = F.execution(F.test("test2"), configurationOpt = Some(DummyData.Configuration1))
    val inBatch = F.batch(executions = List(inExecution1))
    val (_, batchRecorder, _) = getTestContext()

    val batch = batchRecorder.recordBatch(inBatch)

    batch.configurationOpt should equal(Some(DummyData.Configuration1))
  }

  "Submitting a batch without a configuration" should "not have a configuration inferred from the executions if they have multiple configurations" in {
    val inExecution1 = F.execution(F.test("test1"), configurationOpt = Some(DummyData.Configuration1))
    val inExecution2 = F.execution(F.test("test2"), configurationOpt = Some(DummyData.Configuration2))
    val inBatch = F.batch(executions = List(inExecution1, inExecution2), configurationOpt = None)
    val (_, batchRecorder, _) = getTestContext()

    val batch = batchRecorder.recordBatch(inBatch)

    batch.configurationOpt should equal(None)
  }

  "Submitting a batch with a configuration" should "be the default configuration for executions in that batch" in {
    val inExecution1 = F.execution(F.test(), configurationOpt = None)
    val inBatch = F.batch(executions = List(inExecution1), configurationOpt = Some(DummyData.Configuration1))
    val (dao, batchRecorder, _) = getTestContext()

    val batch = batchRecorder.recordBatch(inBatch)

    val List(execution) = dao.getEnrichedExecutionsInBatch(batch.id)
    execution.execution.configuration should equal(DummyData.Configuration1)
    
  }

  private def checkBatchDataWasCaptured(batch: Batch, inBatch: Incoming.Batch) {
    batch.nameOpt should equal(inBatch.nameOpt)
    batch.urlOpt should equal(inBatch.urlOpt)
    batch.passCount should equal(inBatch.executions.count(_.passed))
    batch.failCount should equal(inBatch.executions.count(_.failed))
    batch.totalCount should equal(inBatch.executions.size)
    batch.durationOpt should equal(inBatch.durationOpt)
    batch.passed should be(inBatch.executions.forall(_.passed))
    batch.durationOpt should equal(batch.durationOpt)
    for (incomingExecutionTime ← inBatch.executionTimeOpt)
      batch.executionTime should be(incomingExecutionTime)
  }

  private def checkExecutionDataWasCaptured(execution: EnrichedExecution, inExecution: Incoming.Execution) {
    execution.durationOpt should equal(inExecution.durationOpt)
    execution.passed should equal(inExecution.passed)
    execution.summaryOpt should equal(inExecution.summaryOpt)
    for (incomingExecutionTime ← inExecution.executionTimeOpt)
      execution.executionTime should be(incomingExecutionTime)
    execution.configuration should equal(inExecution.configurationOpt.getOrElse(Configuration.Default))
  }

  private def checkTestDataWasCaptured(test: Test, inTest: Incoming.Test) {
    test.name should equal(inTest.name)
    test.groupOpt should equal(inTest.groupOpt)
  }

  private def getTestContext(clock: Clock = FakeClock()): (Dao, BatchRecorder, AnalysisService) = {
    val dao: Dao = new MockDao
    val analysisService = new AnalysisService(dao, clock, async = false)
    val batchRecorder = new BatchRecorder(dao, clock, analysisService)
    (dao, batchRecorder, analysisService)
  }

  private def findExecution(dao: Dao, batchId: Id[Batch], qualifiedName: QualifiedName): EnrichedExecution =
    dao.getEnrichedExecutionsInBatch(batchId).find { _.qualifiedName == qualifiedName }.getOrElse(
      throw new NoSuchElementException("Could not find test"))

}