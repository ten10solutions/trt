package com.thetestpeople.trt.service

import org.junit.runner.RunWith
import org.scalatest._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.service._
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.model.impl.DummyData
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BatchRecorderTest extends FlatSpec with Matchers {

  "Submitting a batch with a single test" should "correctly capture all input data" in {
    val serviceBundle = TestServiceFactory.setup()
    val dao = serviceBundle.dao
    val inTest = F.test(DummyData.TestName, Some(DummyData.Group), categories = Seq(DummyData.Category))
    val inExecution = F.execution(inTest,
      passed = true,
      summaryOpt = Some(DummyData.Summary),
      logOpt = Some(DummyData.Log),
      executionTimeOpt = Some(DummyData.ExecutionTime),
      durationOpt = Some(DummyData.Duration),
      configurationOpt = Some(DummyData.Configuration1))
    val inBatch = F.batch(
      urlOpt = Some(DummyData.BuildUrl),
      nameOpt = Some(DummyData.BatchName),
      logOpt = Some(DummyData.Log),
      executionTimeOpt = Some(DummyData.ExecutionTime),
      durationOpt = Some(DummyData.Duration),
      executions = List(inExecution))

    val batch = serviceBundle.batchRecorder.recordBatch(inBatch)

    val List(execution) = dao.getEnrichedExecutionsInBatch(batch.id)
    val List(EnrichedTest(test, _, _)) = dao.getAnalysedTests()
    val categories = dao.getCategories(test.id)
    checkBatchDataWasCaptured(batch, inBatch)
    checkExecutionDataWasCaptured(execution, inExecution)
    checkTestDataWasCaptured(test, categories, inTest)
    execution.batchId should equal(batch.id)
    execution.testId should equal(test.id)
  }

  "Submitting a batch with multiple tests" should "capture all the input data" in {
    val serviceBundle = TestServiceFactory.setup()
    val dao = serviceBundle.dao
    val inTest1 = F.test("test1")
    val inExecution1 = F.execution(inTest1)
    val inTest2 = F.test("test2")
    val inExecution2 = F.execution(inTest2)
    val inBatch = F.batch(executions = List(inExecution1, inExecution2))

    val batch = serviceBundle.batchRecorder.recordBatch(inBatch)

    val _executions = dao.getEnrichedExecutionsInBatch(batch.id, passedFilterOpt = None)
    _executions.size should be(2)
    val execution1 = findExecution(dao, batch.id, inTest1.qualifiedName)
    val execution2 = findExecution(dao, batch.id, inTest2.qualifiedName)
    val Some(EnrichedTest(test1, _, _)) = dao.getEnrichedTest(execution1.testId)
    val Some(EnrichedTest(test2, _, _)) = dao.getEnrichedTest(execution2.testId)
    val categories1 = dao.getCategories(test1.id)
    val categories2 = dao.getCategories(test2.id)
    checkBatchDataWasCaptured(batch, inBatch)
    checkExecutionDataWasCaptured(execution1, inExecution1)
    checkExecutionDataWasCaptured(execution2, inExecution2)
    checkTestDataWasCaptured(test1, categories1, inTest1)
    checkTestDataWasCaptured(test2, categories2, inTest2)
  }

  "Submitting a batch with a single failing test" should "record the batch as failing" in {
    val inExecution = F.execution(F.test("testSomething"), passed = false)
    val inBatch = F.batch(executions = List(inExecution))
    val batchRecorder = TestServiceFactory.setup().batchRecorder

    val batch = batchRecorder.recordBatch(inBatch)

    batch.passed should be(false)
  }

  "Submitting a batch without a timestamp" should "be given the current time" in {
    val inExecution = F.execution(F.test("testSomething"), executionTimeOpt = None)
    val inBatch = F.batch(executions = List(inExecution), executionTimeOpt = None)
    val now = new DateTime
    val serviceBundle = TestServiceFactory.setup(clock = FakeClock(now))

    val batch = serviceBundle.batchRecorder.recordBatch(inBatch)
    batch.executionTime should equal(now)
    val List(execution) = serviceBundle.dao.getEnrichedExecutionsInBatch(batch.id)
    execution.executionTime should equal(now)
  }

  "Submitting a batch without a configuration" should "have a configuration inferred from the executions if they are all the same" in {
    val inExecution1 = F.execution(F.test("test1"), configurationOpt = Some(DummyData.Configuration1))
    val inExecution2 = F.execution(F.test("test2"), configurationOpt = Some(DummyData.Configuration1))
    val inBatch = F.batch(executions = List(inExecution1))
    val batchRecorder = TestServiceFactory.setup().batchRecorder

    val batch = batchRecorder.recordBatch(inBatch)

    batch.configurationOpt should equal(Some(DummyData.Configuration1))
  }

  "Submitting a batch without a configuration" should "not have a configuration inferred from the executions if they have multiple configurations" in {
    val inExecution1 = F.execution(F.test("test1"), configurationOpt = Some(DummyData.Configuration1))
    val inExecution2 = F.execution(F.test("test2"), configurationOpt = Some(DummyData.Configuration2))
    val inBatch = F.batch(executions = List(inExecution1, inExecution2), configurationOpt = None)
    val batchRecorder = TestServiceFactory.setup().batchRecorder

    val batch = batchRecorder.recordBatch(inBatch)

    batch.configurationOpt should equal(None)
  }

  "Submitting a batch with a configuration" should "be the default configuration for executions in that batch" in {
    val inExecution = F.execution(F.test(), configurationOpt = None)
    val inBatch = F.batch(executions = List(inExecution), configurationOpt = Some(DummyData.Configuration1))
    val serviceBundle = TestServiceFactory.setup()
    val dao = serviceBundle.dao

    val batch = serviceBundle.batchRecorder.recordBatch(inBatch)

    val List(execution) = dao.getEnrichedExecutionsInBatch(batch.id)
    execution.execution.configuration should equal(DummyData.Configuration1)
  }

  "Submitting tests with categories" should "not clobber user-specified categories" in {
    val serviceBundle = TestServiceFactory.setup()
    val service = serviceBundle.service

    def addBatch(categories: Seq[String]) {
      val inTest = F.test(categories = categories)
      val inExecution = F.execution(inTest)
      val inBatch = F.batch(executions = List(inExecution))
      serviceBundle.batchRecorder.recordBatch(inBatch)
    }

    addBatch(categories = Seq())
    val Seq(testId) = serviceBundle.dao.getTestIds()
    service.addCategory(testId, "UserCategory1")
    service.addCategory(testId, "UserCategory2")

    addBatch(categories = Seq("ImportCategory1", "ImportCategory2", "UserCategory2"))
    val categories = service.getTestAndExecutions(testId).get.categories
    categories should contain theSameElementsAs Seq("UserCategory1", "UserCategory2", "ImportCategory1", "ImportCategory2")

    addBatch(categories = Seq())
    val categoriesAgain = service.getTestAndExecutions(testId).get.categories
    categoriesAgain should contain theSameElementsAs Seq("UserCategory1", "UserCategory2")
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

  private def checkTestDataWasCaptured(test: Test, categories: Seq[TestCategory], inTest: Incoming.Test) {
    test.name should equal(inTest.name)
    test.groupOpt should equal(inTest.groupOpt)
    val expectedCategories = inTest.categories.map(c ⇒ TestCategory(test.id, c, isUserCategory = false))
    expectedCategories should contain theSameElementsAs (categories)
  }

  private def findExecution(dao: Dao, batchId: Id[Batch], qualifiedName: QualifiedName): EnrichedExecution =
    dao.getEnrichedExecutionsInBatch(batchId).find { _.qualifiedName == qualifiedName }.getOrElse(
      throw new NoSuchElementException("Could not find test"))

}