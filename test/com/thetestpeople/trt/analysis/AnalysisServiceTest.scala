package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.MockDao
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import com.thetestpeople.trt.service.FakeClock

@RunWith(classOf[JUnitRunner])
class AnalysisServiceTest extends FlatSpec with Matchers {

  "Analysis service" should "record an analysis result" in {
    val Setup(dao, clock, analysisService) = setUp()
    dao.updateSystemConfiguration(SystemConfiguration(failureDurationThreshold = 6.hours, failureCountThreshold = 3))
    val batchId = dao.newBatch(F.batch())
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.newExecution(F.execution(testId = testId, batchId = batchId, passed = false))

    analysisService.analyseTest(testId)

    val Some(EnrichedTest(test, Some(analysis), _)) = dao.getEnrichedTest(testId)
    analysis.status should equal(TestStatus.Warning)
    analysis.consecutiveFailures should equal(1)
    analysis.whenAnalysed should equal(clock.now)
  }

  it should "cache historical test results" in {
    val Setup(dao, clock, analysisService) = setUp()
    val batchId = dao.newBatch(F.batch())
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.newExecution(F.execution(testId = testId, batchId = batchId, passed = true))
    analysisService.getHistoricalTestCountsByConfig should equal(Map())

    analysisService.analyseAllExecutions()

    val allCounts = analysisService.getHistoricalTestCountsByConfig(Configuration.Default)
    val mostRecentCounts = allCounts.counts.last.testCounts
    mostRecentCounts should equal(TestCounts(passed = 1))
  }

  it should "let you clear the cache" in {
    val Setup(dao, clock, analysisService) = setUp()
    val batchId = dao.newBatch(F.batch())
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.newExecution(F.execution(testId = testId, batchId = batchId, passed = true))
    analysisService.analyseAllExecutions()

    analysisService.clearHistoricalTestCounts()

    analysisService.getHistoricalTestCountsByConfig should equal(Map())
  }

  private def setUp() = {
    val dao = new MockDao
    val clock = FakeClock()
    Setup(dao, clock, new AnalysisService(dao, clock))
  }

  private case class Setup(dao: MockDao, clock: FakeClock, analysisService: AnalysisService)

}