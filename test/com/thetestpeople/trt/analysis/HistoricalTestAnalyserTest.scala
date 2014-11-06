package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import java.util.concurrent.atomic.AtomicInteger
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.service._
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat

@RunWith(classOf[JUnitRunner])
class HistoricalTestAnalyserTest extends FlatSpec with Matchers {

  "Historical analyser" should "give same result as ordinary test analyser for its most recent results" in {
    val analysisConfig = analysisConfiguration(
      passDurationThreshold = 6.hours, passCountThreshold = 2,
      failureDurationThreshold = 6.hours, failureCountThreshold = 2)

    val executions: Seq[Execution] = Seq(
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
    val execution1 = execution(passed = true, executionTime = date("2014-11-01T12:00:00Z"))
    val execution2 = execution(passed = true, executionTime = date("2014-11-02T12:00:00Z"))
    val execution3 = execution(passed = false, executionTime = date("2014-11-03T12:00:00Z"))
    val execution4 = execution(passed = false, executionTime = date("2014-11-04T12:00:00Z"))
    val executions = Seq(execution1, execution2, execution3, execution4).map(executionLite)

    val Seq(counts2, counts3, counts4, counts5) = analyse(executions, analysisConfig)(Configuration.Default).counts

    counts2.when.getMillis should equal(date("2014-11-02T00:00:00Z").getMillis)
    counts2.testCounts should equal(TestCounts(warning = 1))

    counts3.when.getMillis should equal(date("2014-11-03T00:00:00Z").getMillis)
    counts3.testCounts should equal(TestCounts(passed = 1))

    counts4.when.getMillis should equal(date("2014-11-04T00:00:00Z").getMillis)
    counts4.testCounts should equal(TestCounts(warning = 1))

    counts5.when.getMillis should equal(date("2014-11-05T00:00:00Z").getMillis)
    counts5.testCounts should equal(TestCounts(failed = 1))
  }

  it should "give correct results with a single execution" in {
    val analysisConfig = analysisConfiguration(
      passDurationThreshold = 0.hours, passCountThreshold = 1)
    val executions = Seq(executionLite(execution(passed = true, executionTime = date("2014-11-01T12:00:00Z"))))
    val Seq(counts) = analyse(executions, analysisConfig)(Configuration.Default).counts

    counts.when.getMillis should equal(date("2014-11-02T00:00:00Z").getMillis)
    counts.testCounts should equal(TestCounts(passed = 1))
  }

  private def testCounts(pass: Int = 0, warn: Int = 0, fail: Int = 0) = TestCounts(pass, warn, fail)

  private def calculateTestCountsViaRegularAnalyser(analysisConfig: AnalysisConfiguration, executions: Seq[Execution]): TestCounts = {
    val testAnalyser = new TestAnalyser(new FakeClock, analysisConfig)
    val countsByStatus = executions.groupBy(_.testId).flatMap(x ⇒ testAnalyser.analyse(x._2)).groupBy(_.status).mapValues(_.size)
    TestCounts(
      passed = countsByStatus(TestStatus.Healthy),
      warning = countsByStatus(TestStatus.Warning),
      failed = countsByStatus(TestStatus.Broken))
  }

  private def calculateTestCountsViaHistoricalAnalyser(analysisConfig: AnalysisConfiguration, executions: Seq[Execution]): TestCounts = {
    analyse(executions.map(executionLite), analysisConfig)(Configuration.Default).counts.last.testCounts
  }

  private def analyse(executions: Seq[ExecutionLite], analysisConfig: AnalysisConfiguration) = {
    val executionIntervalsByConfig =
      for ((configuration, configExecutions) ← executions.groupBy(_.configuration))
        yield configuration -> executionInterval(configExecutions)
    val historicalAnalyser = new HistoricalTestAnalyser(executionIntervalsByConfig, analysisConfig, DateTimeZone.UTC)
    val executionsIterator = executions.sortBy(e ⇒ (e.configuration, e.testId, e.executionTime)).iterator
    historicalAnalyser.process(executionsIterator)
  }

  private def executionInterval(executions: Seq[ExecutionLite]): Interval = {
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

  private def date(s: String): DateTime = ISODateTimeFormat.dateTimeParser.parseDateTime(s)

}