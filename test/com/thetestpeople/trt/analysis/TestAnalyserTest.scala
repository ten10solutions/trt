package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

import java.util.concurrent.atomic.AtomicInteger

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service._
import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class TestAnalyserTest extends FlatSpec with Matchers {

  "A test which has passed for sufficiently long and frequently" should "be classified as a pass" in {

    val testAnalyser = getTestAnalyser(passDurationThreshold = 6.hours, passCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      pass(1.hour.ago),
      pass(6.hours.ago),
      pass(12.hours.ago),
      fail(24.hours.ago))

    analysis.status should be(TestStatus.Healthy)

  }

  "A test which has not passed for sufficiently long" should "be classified as a warning" in {

    val testAnalyser = getTestAnalyser(passDurationThreshold = 6.hours, passCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      pass(1.hour.ago),
      pass(2.hours.ago),
      pass(3.hours.ago),
      fail(24.hours.ago))

    analysis.status should be(TestStatus.Warning)

  }

  "A test which has not passed sufficiently frequently" should "be classified as a warning" in {

    val testAnalyser = getTestAnalyser(passDurationThreshold = 6.hours, passCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      pass(1.hour.ago),
      pass(12.hours.ago),
      fail(24.hours.ago))

    analysis.status should be(TestStatus.Warning)

  }

  "A test which has failed for sufficiently long and frequently" should "be classifed as a failure" in {

    val testAnalyser = getTestAnalyser(failureDurationThreshold = 6.hours, failureCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      fail(1.hour.ago),
      fail(6.hours.ago),
      fail(12.hours.ago),
      pass(24.hours.ago))

    analysis.status should be(TestStatus.Broken)
  }

  "A test which has not failed for sufficiently long" should "be classifed as a warning" in {

    val testAnalyser = getTestAnalyser(failureDurationThreshold = 6.hours, failureCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      fail(1.hour.ago),
      fail(2.hours.ago),
      fail(3.hours.ago),
      pass(24.hours.ago))

    analysis.status should be(TestStatus.Warning)
  }

  "A test which has not failed sufficiently frequently" should "be classifed as a warning" in {

    val testAnalyser = getTestAnalyser(failureDurationThreshold = 6.hours, failureCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      fail(1.hour.ago),
      fail(12.hours.ago),
      pass(24.hours.ago))

    analysis.status should be(TestStatus.Warning)
  }

  "A test which has never failed but has not reached the pass threshold" should "be classified as a warning" in {

    val testAnalyser = getTestAnalyser(passDurationThreshold = 6.hours, passCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      pass(1.hour.ago))

    analysis.status should be(TestStatus.Warning)

  }

  "A test which has never passed but has not reached the failure threshold" should "be classified as a warning" in {

    val testAnalyser = getTestAnalyser(failureDurationThreshold = 6.hours, failureCountThreshold = 3)

    val analysis = testAnalyser.analyse(
      failed(1.hour.ago))

    analysis.status should be(TestStatus.Warning)

  }

  "If the fail threshold is one, and failure duration is zero, then a test which has failed most recently" should "be classed as a failure" in {

    val analysis = getTestAnalyser(failureDurationThreshold = 0.minutes, failureCountThreshold = 1).analyse(
      fail(1.hour.ago),
      pass(12.hours.ago))

    analysis.status should be(TestStatus.Broken)
  }

  "Analyser" should "count zero consecutive failures" in {

    val analysis = getTestAnalyser().analyse(
      pass(1.hour.ago),
      fail(12.hours.ago),
      pass(24.hours.ago))

    analysis.consecutiveFailures should be(0)
  }

  "A test's weather" should "be sunny if it has always passed" in {

    val analysis = getTestAnalyser().analyse(
      pass(1.hour.ago),
      pass(12.hours.ago),
      pass(24.hours.ago))

    analysis.weather should be(1.0)
  }

  "Analyser" should "count the correct number of consecutive failures" in {

    val analysis = getTestAnalyser().analyse(
      fail(1.hour.ago),
      fail(12.hours.ago),
      pass(24.hours.ago),
      fail(1.week.ago))

    analysis.consecutiveFailures should be(2)
  }

  "A test's weather" should "be stormy if it has always failed" in {

    val analysis = getTestAnalyser().analyse(
      fail(1.hour.ago),
      fail(12.hours.ago),
      fail(24.hours.ago))

    analysis.weather should be(0.0)
  }

  "The most recent passing and failing executions" should "be identified correctly" in {

    val execution1 = fail(1.hour.ago)
    val execution2 = pass(2.hour.ago)
    val execution3 = fail(3.hour.ago)
    val execution4 = pass(4.hour.ago)
    val analysis = getTestAnalyser().analyse(
      execution1,
      execution2,
      execution3,
      execution4)

    analysis.lastFailedExecutionOpt.map(_.id) should equal(Some(execution1.id))
    analysis.lastPassedExecutionOpt.map(_.id) should equal(Some(execution2.id))
  }

  "No most recent pass" should "be found if never passed" in {

    val analysis = getTestAnalyser().analyse(
      fail(1.hour.ago),
      fail(2.hours.ago))

    analysis.lastPassedExecutionOpt should equal(None)
  }

  "No most recent fail" should "be found if never failed" in {

    val analysis = getTestAnalyser().analyse(
      pass(1.hour.ago))

    analysis.lastFailedExecutionOpt should equal(None)
  }

  "Analysis" should "should calculate how long a test has been failing for" in {
    val startedFailing = 1.week.ago

    val analysis = getTestAnalyser().analyse(
      fail(1.hour.ago),
      fail(2.hours.ago),
      fail(startedFailing))

    analysis.failingSinceOpt should equal(Some(startedFailing))
  }

  "Analysis" should "calculate median duration of a test" in {
    val analysis = getTestAnalyser().analyse(
      execution(durationOpt = None),
      execution(durationOpt = Some(5.minutes)),
      execution(durationOpt = Some(6.minutes)),
      execution(durationOpt = Some(12.hours)))

    analysis.medianDurationOpt should be(Some(6.minutes: Duration))
  }

  implicit class RichTestAnalyser(testAnalyser: TestAnalyser) {

    def analyse(executions: Execution*): TestAnalysis = testAnalyser.analyse(executions.toList).get

  }

  private def getTestAnalyser(
    failureDurationThreshold: Duration = 6.hours,
    failureCountThreshold: Int = 3,
    passDurationThreshold: Duration = 6.hours,
    passCountThreshold: Int = 3,
    clock: Clock = FakeClock()) =
    new TestAnalyser(
      clock,
      SystemConfiguration(
        failureDurationThreshold = failureDurationThreshold,
        failureCountThreshold = failureCountThreshold,
        passDurationThreshold = passDurationThreshold,
        passCountThreshold = passCountThreshold))

  private def pass(executionTime: DateTime) = execution(passed = true, executionTime = executionTime)
  private def fail(executionTime: DateTime) = execution(passed = false, executionTime = executionTime)
  private def failed(executionTime: DateTime) = fail(executionTime)

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

}