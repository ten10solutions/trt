package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicInteger

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.service._

import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class HistoricalTestAnalyserTest extends FlatSpec with Matchers {

  "Historical analyser" should "give same result as ordinary test analyser for its most recent results" in {
    val analysisConfig = analysisConfiguration(
      passDurationThreshold = 6.hours, passCountThreshold = 2,
      failureDurationThreshold = 6.hours, failureCountThreshold = 2)

    val executions: List[Execution] = List(
      execution(testId = Id(1), passed = true, executionTime = 1.day.ago),
      execution(testId = Id(1), passed = true, executionTime = 2.days.ago),
      execution(testId = Id(2), passed = false, executionTime = 1.day.ago),
      execution(testId = Id(2), passed = false, executionTime = 2.days.ago),
      execution(testId = Id(3), passed = true, executionTime = 1.day.ago),
      execution(testId = Id(4), passed = false, executionTime = 1.day.ago))

    val testCountsViaRegularAnalyser = calculateTestCountsViaRegularAnalyser(analysisConfig, executions)
    val testCountsViaHistoricalAnalyser = calculateTestCountsViaHistoricalAnalyser(analysisConfig, executions)

    testCountsViaHistoricalAnalyser should equal(testCountsViaRegularAnalyser)
  }

  it should "give correct results over time" in {
    val analysisConfig = analysisConfiguration(
      passDurationThreshold = 6.hours, passCountThreshold = 2,
      failureDurationThreshold = 6.hours, failureCountThreshold = 2)
    val execution1 = execution(passed = true, executionTime = 4.days.ago)
    val execution2 = execution(passed = true, executionTime = 3.days.ago)
    val execution3 = execution(passed = false, executionTime = 2.days.ago)
    val execution4 = execution(passed = false, executionTime = 1.day.ago)
    val executions = List(execution1, execution2, execution3, execution4).map(executionLite)
    val sampleSize = executions.size + 1 // this should cause the samples to fall between the executions

    val List(counts1, counts2, counts3, counts4, counts5) = analyse(executions, analysisConfig, sampleSize = executions.size + 1)(Configuration.Default).counts

    counts1.when should equal(execution1.executionTime)
    counts1.testCounts should equal(TestCounts(warning = 1))

    counts2.when > execution1.executionTime should be(true)
    counts2.when < execution2.executionTime should be(true)
    counts2.testCounts should equal(TestCounts(warning = 1))

    counts3.when > execution2.executionTime should be(true)
    counts3.when < execution3.executionTime should be(true)
    counts3.testCounts should equal(TestCounts(passed = 1))

    counts4.when > execution3.executionTime should be(true)
    counts4.when < execution4.executionTime should be(true)
    counts4.testCounts should equal(TestCounts(warning = 1))

    counts5.when should equal(execution4.executionTime)
    counts5.testCounts should equal(TestCounts(failed = 1))
  }

  it should "give correct results with a single execution" in {
    val analysisConfig = analysisConfiguration(
      passDurationThreshold = 0.hours, passCountThreshold = 1)
    val execution1 = execution(passed = true)
    val executions = List(executionLite(execution1))
    val List(counts) = analyse(executions, analysisConfig)(Configuration.Default).counts

    counts.when should equal(execution1.executionTime)
    counts.testCounts should equal(TestCounts(passed = 1))
  }

  private def testCounts(pass: Int = 0, warn: Int = 0, fail: Int = 0) = TestCounts(pass, warn, fail)

  private def calculateTestCountsViaRegularAnalyser(analysisConfig: AnalysisConfiguration, executions: List[Execution]): TestCounts = {
    val testAnalyser = new TestAnalyser(new FakeClock, analysisConfig)
    val countsByStatus = executions.groupBy(_.testId).flatMap(x ⇒ testAnalyser.analyse(x._2)).groupBy(_.status).mapValues(_.size)
    TestCounts(
      passed = countsByStatus(TestStatus.Pass),
      warning = countsByStatus(TestStatus.Warn),
      failed = countsByStatus(TestStatus.Fail))
  }

  private def calculateTestCountsViaHistoricalAnalyser(analysisConfig: AnalysisConfiguration, executions: List[Execution]): TestCounts = {
    analyse(executions.map(executionLite), analysisConfig)(Configuration.Default).counts.last.testCounts
  }

  private def analyse(executions: List[ExecutionLite], analysisConfig: AnalysisConfiguration, sampleSize: Int = 200) = {
    val executionIntervalsByConfig =
      for ((configuration, configExecutions) ← executions.groupBy(_.configuration))
        yield configuration -> executionInterval(configExecutions)
    val historicalAnalyser = new HistoricalTestAnalyser(executionIntervalsByConfig, analysisConfig, sampleSize)
    val executionsIterator = executions.sortBy(e ⇒ (e.configuration, e.testId, e.executionTime)).iterator
    historicalAnalyser.process(executionsIterator)
  }

  private def executionInterval(executions: List[ExecutionLite]): Interval = {
    val start = executions.minBy(_.executionTime).executionTime
    val end = executions.maxBy(_.executionTime).executionTime
    new Interval(start, end)
  }

  private def analysisConfiguration(
    failureDurationThreshold: Duration = 6.hours,
    failureCountThreshold: Int = 3,
    passDurationThreshold: Duration = 6.hours,
    passCountThreshold: Int = 3,
    clock: Clock = FakeClock()): AnalysisConfiguration =
    SystemConfiguration(
      failureDurationThreshold = failureDurationThreshold,
      failureCountThreshold = failureCountThreshold,
      passDurationThreshold = passDurationThreshold,
      passCountThreshold = passCountThreshold)

  private val uniqueIdSource = new AtomicInteger(1)
  private def execution(
    passed: Boolean = true,
    executionTime: DateTime = new DateTime,
    durationOpt: Option[Duration] = None,
    configuration: Configuration = Configuration.Default,
    testId: Id[Test] = Id[Test](0)): Execution =
    Execution(
      id = Id[Execution](uniqueIdSource.getAndIncrement()),
      batchId = Id[Batch](0),
      testId = testId,
      executionTime = executionTime,
      durationOpt = durationOpt,
      summaryOpt = None,
      passed = passed,
      configuration = configuration)

  def executionLite(execution: Execution): ExecutionLite =
    ExecutionLite(
      configuration = execution.configuration,
      testId = execution.testId,
      executionTime = execution.executionTime,
      passed = execution.passed)

}