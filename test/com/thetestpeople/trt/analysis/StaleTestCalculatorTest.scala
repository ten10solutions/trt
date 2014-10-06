package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.DummyData
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import org.joda.time.Duration

@RunWith(classOf[JUnitRunner])
class StaleTestCalculatorTest extends FlatSpec with Matchers {

  "Stale test calculator" should "let you identify tests that haven't been run for a while" in {
    val test1 = test(5.hours.ago)
    val test2 = test(6.hours.ago)
    val test3 = test(7.hours.ago)
    val test4 = test(8.hours.ago)
    val test5 = test(3.weeks.ago)

    val (Some(executionTimeMad), staleTests) = new StaleTestCalculator().findStaleTests(Seq(test1, test2, test3, test4, test5))

    executionTimeMad.medianExecutionTime should equal(test3.analysisOpt.get.lastExecutionTime)
    staleTests should equal(Seq(test5))
  }

  private def test(executionTime: DateTime): TestAndAnalysis = {
    val test = F.test()
    val batch = F.batch()
    val execution = F.execution(batch.id, test.id)
    val analysis: Analysis = F.analysis(test.id, lastPassedExecutionIdOpt = Some(execution.id), lastPassedTimeOpt = Some(executionTime))
    TestAndAnalysis(test, Some(analysis), None)
  }

}
