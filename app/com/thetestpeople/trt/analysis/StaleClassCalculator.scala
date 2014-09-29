package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.TestAndAnalysis
import org.joda.time.DateTime
import org.joda.time.Duration
import com.github.nscala_time.time.Imports._

case class ExecutionTimeMAD(medianExecutionTime: DateTime, medianDeviation: Duration)

object StaleTestCalculator {

  val ConsistencyConstant = 1.4826 // http://eurekastatistics.com/using-the-median-absolute-deviation-to-find-outliers

}

class StaleTestCalculator(deviationsCutoff: Int = 3) {

  import StaleTestCalculator._

  def findStaleTests(analysedTests: Seq[TestAndAnalysis]): (Option[ExecutionTimeMAD], Seq[TestAndAnalysis]) = {
    val madOpt = calculateExecutionTimeMAD(analysedTests)
    val tests = madOpt.toSeq.flatMap { mad ⇒
      findOutliers(analysedTests, mad)
    }
    (madOpt, tests)
  }

  private def calculateMedian(items: Seq[Double]): Option[Double] = {
    val sortedItems = items.sorted
    val count = items.size
    if (count == 0)
      None
    else if (count % 2 == 0)
      Some((sortedItems(count / 2) + sortedItems(count / 2 - 1)) / 2.0)
    else
      Some(sortedItems(count / 2))
  }

  private def calculateExecutionTimeMAD(analysedTests: Seq[TestAndAnalysis]) = {
    val lastExecutionTimes = analysedTests.flatMap(_.analysisOpt).map(_.lastExecutionTime.getMillis.toDouble)
    for {
      median ← calculateMedian(lastExecutionTimes)
      absoluteDeviations = lastExecutionTimes.map(t ⇒ math.abs(t - median))
      medianDeviation ← calculateMedian(absoluteDeviations)
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
        if deviation > mad.medianDeviation.getMillis * deviationsCutoff
      } yield test
    tests.sortBy(_.test.name).sortBy(_.test.groupOpt).sortBy(_.analysisOpt.get.lastExecutionTime)
  }

}