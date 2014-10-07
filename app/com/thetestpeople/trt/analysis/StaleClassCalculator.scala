package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.TestAndAnalysis
import org.joda.time.DateTime
import org.joda.time.Duration
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.utils.StatsUtils

case class ExecutionTimeMAD(medianExecutionTime: DateTime, medianDeviation: Duration)

object StaleTestCalculator {

  val ConsistencyConstant = 1.4826 // http://eurekastatistics.com/using-the-median-absolute-deviation-to-find-outliers

}

/**
 * @param deviationsThreshold -- how many standard deviations (or equivalent) we need to be before the median execution time to classify a test as stale
 * @param absoluteThreshold -- how much earlier than the median execution time we need to be (in absolute duration) to classify a test as stale 
 */
class StaleTestCalculator(deviationsThreshold: Int = 3, absoluteThreshold: Duration = 48.hours) {

  import StaleTestCalculator._

  def findStaleTests(analysedTests: Seq[TestAndAnalysis]): (Option[ExecutionTimeMAD], Seq[TestAndAnalysis]) = {
    val madOpt = calculateExecutionTimeMAD(analysedTests)
    val tests = madOpt.toSeq.flatMap { mad ⇒
      findOutliers(analysedTests, mad)
    }
    (madOpt, tests)
  }

  private def calculateExecutionTimeMAD(analysedTests: Seq[TestAndAnalysis]) = {
    val lastExecutionTimes = analysedTests.flatMap(_.analysisOpt).map(_.lastExecutionTime.getMillis.toDouble)
    for {
      median ← StatsUtils.median(lastExecutionTimes)
      absoluteDeviations = lastExecutionTimes.map(t ⇒ math.abs(t - median))
      medianDeviation ← StatsUtils.median(absoluteDeviations)
    } yield ExecutionTimeMAD(
      medianExecutionTime = new DateTime(median.toLong),
      medianDeviation = Duration.millis((medianDeviation * ConsistencyConstant).toLong))
  }

  private def findOutliers(analysedTests: Seq[TestAndAnalysis], mad: ExecutionTimeMAD): Seq[TestAndAnalysis] = {
    val tests =
      for {
        test ← analysedTests
        analysis ← test.analysisOpt
        deviation = mad.medianExecutionTime.getMillis - analysis.lastExecutionTime.getMillis
        if deviation > mad.medianDeviation.getMillis * deviationsThreshold
        if analysis.lastExecutionTime < mad.medianExecutionTime - absoluteThreshold
      } yield test
    tests.sortBy(_.test.name).sortBy(_.test.groupOpt).sortBy(_.analysisOpt.get.lastExecutionTime)
  }

}