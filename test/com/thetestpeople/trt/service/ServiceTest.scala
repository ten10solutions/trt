package com.thetestpeople.trt.service

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.utils.http._
import com.thetestpeople.trt.mother.{ IncomingFactory ⇒ F }
import com.thetestpeople.trt.model.impl.MockDao
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.jenkins.importer.JenkinsImportStatusManager
import com.thetestpeople.trt.jenkins.importer.FakeJenkinsImportQueue
import com.thetestpeople.trt.service.indexing.LuceneLogIndexer

@RunWith(classOf[JUnitRunner])
class ServiceTest extends FlatSpec with ShouldMatchers {

  private implicit class RichService(service: Service) {

    def getStatus(testId: Id[Test]): TestStatus = {
      val Some(testAndExecutions) = service.getTestAndExecutions(testId)
      val Some(analysis) = testAndExecutions.test.analysisOpt
      analysis.status
    }

    def getEnrichedExecutionsInBatch(batchId: Id[Batch]): List[EnrichedExecution] =
      service.getBatchAndExecutions(batchId).toList.flatMap(_.executions)

    def getTestIdsInBatch(batchId: Id[Batch]): List[Id[Test]] =
      getEnrichedExecutionsInBatch(batchId).map(_.testId).distinct

    def getExecutionIdsInBatch(batchId: Id[Batch]): List[Id[Execution]] =
      getEnrichedExecutionsInBatch(batchId).map(_.id).distinct

  }

  "Service" should "let you add a batch" in {
    val service = setup().service
    val inBatch = F.batch()

    val batchId = service.addBatch(inBatch)

    val List(batch) = service.getBatches()
    batch.id should equal(batchId)
  }

  it should "allow you to fetch a batch and its executions" in {
    val service = setup().service
    val inTest = F.test(DummyData.TestName, Some(DummyData.Group))
    val inExecution = F.execution(inTest)
    val inBatch = F.batch(executions = List(inExecution), logOpt = Some(DummyData.Log))
    val batchId = service.addBatch(inBatch)

    val Some(BatchAndExecutions(batch, List(execution), Some(log), None, None)) = service.getBatchAndExecutions(batchId)

    log should equal(DummyData.Log)
    execution.qualifiedName should equal(inTest.qualifiedName)
    batch.id should equal(batchId)
  }

  it should "allow you to fetch all the batches" in {
    val service = setup().service
    val inBatch1 = F.batch()
    val inBatch2 = F.batch()
    val batchId1 = service.addBatch(inBatch1)
    val batchId2 = service.addBatch(inBatch2)

    val batches = service.getBatches()

    batches.map(_.id) should contain theSameElementsAs (List(batchId1, batchId2))
  }

  it should "allow you to fetch a test and its executions, including analysis" in {
    val service = setup().service
    val inBatch = F.batch(executions = List(F.execution(F.test(), passed = true)))
    val batchId = service.addBatch(inBatch)
    val List(testId) = service.getTestIdsInBatch(batchId)
    val List(executionId) = service.getExecutionIdsInBatch(batchId)

    val Some(TestAndExecutions(testAndAnalysis, List(execution), configurations)) = service.getTestAndExecutions(testId)

    val TestAndAnalysis(test, Some(analysis), _) = testAndAnalysis
    analysis.status should equal(TestStatus.Healthy)
    testId should equal(test.id)
    executionId should equal(execution.id)
  }

  it should "let you fetch a paged list of tests and total counts" in {
    val service = setup().service
    service.addBatch(F.batch(executions = List(F.execution(F.test("test1"), passed = true))))
    service.addBatch(F.batch(executions = List(F.execution(F.test("test2"), passed = false))))
    service.addBatch(F.batch(executions = List(F.execution(F.test("test3"), passed = false))))
    service.addBatch(F.batch(executions = List(F.execution(F.test("test4"), passed = true))))
    val (counts, testsAndAnalysis) = service.getTests(startingFrom = 0, limit = 2)
    counts.total should be(4)
    testsAndAnalysis.size should be(2)
  }

  "Deleting batches" should "delete the data and trigger analysis" in {
    val service = setup().service
    service.updateSystemConfiguration(SystemConfiguration(
      passDurationThreshold = 0.minutes, passCountThreshold = 1,
      failureDurationThreshold = 0.minutes, failureCountThreshold = 1))

    def addBatch(passed: Boolean, executionTime: DateTime): Id[Batch] =
      service.addBatch(
        F.batch(executions = List(F.execution(passed = passed, executionTimeOpt = Some(executionTime)))))

    val batchId1 = addBatch(passed = true, executionTime = 2.days.ago)
    val batchId2 = addBatch(passed = false, executionTime = 1.day.ago)
    val List(testId) = service.getTestIdsInBatch(batchId1)

    service.getStatus(testId) should equal(TestStatus.Broken)
    service.getBatches().map(_.id) should contain theSameElementsAs List(batchId1, batchId2)

    service.deleteBatches(List(batchId2))

    service.getStatus(testId) should equal(TestStatus.Healthy)
    service.getBatches().map(_.id) should contain theSameElementsAs List(batchId1)
  }

  "Deleting batches" should "delete its executions from the search index" in {
    val service = setup().service
    def addBatch(): Id[Batch] =
      service.addBatch(F.batch(executions = List(F.execution(logOpt = Some("foo")))))
    val batchId1 = addBatch()
    val batchId2 = addBatch()

    service.searchLogs("foo")._2 should equal(2)

    service.deleteBatches(List(batchId2))

    service.searchLogs("foo")._2 should equal(1)
  }

  "Updating system configuration" should "update the status of tests" in {
    val service = setup().service
    val inTest = F.test()
    val inBatch = F.batch(executions = List(
      F.execution(inTest, passed = false, executionTimeOpt = Some(1.day.ago)),
      F.execution(inTest, passed = false, executionTimeOpt = Some(2.days.ago)),
      F.execution(inTest, passed = true, executionTimeOpt = Some(3.days.ago))))
    val batchId = service.addBatch(inBatch)
    val List(testId) = service.getTestIdsInBatch(batchId)

    val tolerantConfig = SystemConfiguration(failureDurationThreshold = 1.week.toStandardDuration, failureCountThreshold = 100)
    service.updateSystemConfiguration(tolerantConfig)
    service.getStatus(testId) should equal(TestStatus.Warning)

    val intolerantConfig = SystemConfiguration(failureDurationThreshold = 0.minutes, failureCountThreshold = 1)
    service.updateSystemConfiguration(intolerantConfig)
    service.getStatus(testId) should equal(TestStatus.Broken)
  }

  "Execution logs" should "be removed from the search index if the execution is deleted" in {
    val service = setup().service
    val batchId = service.addBatch(F.batch(executions = List(
      F.execution(F.test(), logOpt = Some("foo bar baz")))))
    service.searchLogs("foo")._2 should equal(1)

    service.deleteBatches(List(batchId))

    service.searchLogs("foo")._2 should equal(0)
  }

  private def setup() = TestServiceFactory.setup()

}