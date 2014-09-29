package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.DummyData
import com.github.nscala_time.time.Imports._
import scala.collection.immutable.SortedMap

@RunWith(classOf[JUnitRunner])
class ExecutionVolumeAnalyserTest extends FlatSpec with Matchers {

  "Counting executions" should "work" in {
    val now = new DateTime

    val executions = Seq(
      execution(now, DummyData.Configuration1),
      execution(now, DummyData.Configuration2),
      execution(now - 24.hours, DummyData.Configuration1))

    val result = new ExecutionVolumeAnalyser().process(executions.iterator)

    val today = now.toLocalDate
    val yesterday = (now - 24.hours).toLocalDate
    result.getExecutionVolume(None) should equal(
      Some(ExecutionVolume(SortedMap(today -> 2, yesterday -> 1))))
    result.getExecutionVolume(Some(DummyData.Configuration1)) should equal(
      Some(ExecutionVolume(SortedMap(today -> 1, yesterday -> 1))))
    result.getExecutionVolume(Some(DummyData.Configuration2)) should equal(
      Some(ExecutionVolume(SortedMap(today -> 1))))

  }

  def execution(executionTime: DateTime, configuration: Configuration) =
    ExecutionLite(
      configuration = configuration,
      testId = Id.dummy,
      executionTime = executionTime,
      passed = true)

}