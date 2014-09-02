package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.DummyData
import com.github.nscala_time.time.Imports._

object QuickTestAnalyserTest {

  val Tolerance = 0.001

}

@RunWith(classOf[JUnitRunner])
class QuickTestAnalyserTest extends FlatSpec with Matchers {

  import QuickTestAnalyserTest._

  "Quick Test Analyser" should "let you compute results over time" in {

    val testAnalyser =
      quickTestAnalyser(
        pass(2.days.ago),
        pass(4.days.ago),
        fail(6.days.ago))

    testAnalyser.ignoreExecutionsAfter(1.day.ago)

    val Some(block1) = testAnalyser.getMostRecentPassFailBlock
    block1.passed should be(true)
    block1.count should be(2)
    days(block1.duration) should be(2.0 +- Tolerance)

    testAnalyser.ignoreExecutionsAfter(3.days.ago)

    val Some(block2) = testAnalyser.getMostRecentPassFailBlock
    block2.passed should be(true)
    block2.count should be(1)
    block2.duration.getStandardDays should be(0)

    testAnalyser.ignoreExecutionsAfter(5.days.ago)

    val Some(block3) = testAnalyser.getMostRecentPassFailBlock
    block3.passed should be(false)
    block3.count should be(1)
    block3.duration.getStandardDays should be(0)

  }

  private def quickTestAnalyser(executions: ExecutionLite*) = new QuickTestAnalyser(executions.toArray)

  private def pass(executionTime: DateTime) = execution(passed = true, executionTime = executionTime)
  private def fail(executionTime: DateTime) = execution(passed = false, executionTime = executionTime)

  private def execution(executionTime: DateTime, passed: Boolean) =
    ExecutionLite(
      configuration = DummyData.Configuration1,
      testId = Id[Test](0),
      executionTime = executionTime,
      passed = passed)

  private def days(duration: Duration) = duration.getMillis.toDouble / 1000 / 60 / 60 / 24

}