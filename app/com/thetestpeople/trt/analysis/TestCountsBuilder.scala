package com.thetestpeople.trt.analysis

import org.joda.time.DateTime

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._

/**
 * Mutable store of test counts by configuration, then time.
 */
class TestCountsBuilder(sampleTimesByConfig: Map[Configuration, List[DateTime]]) {

  private var countsMap: Map[Configuration, Map[DateTime, MutableTestCounts]] = Map()

  for ((configuration, sampleTimes) ← sampleTimesByConfig)
    countsMap += (configuration -> sampleTimes.map(t ⇒ t -> new MutableTestCounts).toMap)

  def recordResult(configuration: Configuration, sampleTime: DateTime, status: TestStatus) {
    val mutableCounts = countsMap(configuration)(sampleTime)
    status match {
      case TestStatus.Pass ⇒ mutableCounts.passes += 1
      case TestStatus.Warn ⇒ mutableCounts.warnings += 1
      case TestStatus.Fail ⇒ mutableCounts.failures += 1
    }
  }

  def build(): Map[Configuration, HistoricalTestCountsTimeline] =
    for {
      (configuration, sampleTimeToCounts) ← countsMap
      counts = sampleTimeToCounts.map {
        case (sampleTime, mutableCounts) ⇒ HistoricalTestCounts(sampleTime, mutableCounts.testCounts)
      }.toList.sortBy(_.when)
    } yield configuration -> HistoricalTestCountsTimeline(configuration, counts)

  private class MutableTestCounts {
    var passes: Int = 0
    var warnings: Int = 0
    var failures: Int = 0
    def testCounts: TestCounts = TestCounts(passed = passes, warning = warnings, failed = failures)
  }

}

