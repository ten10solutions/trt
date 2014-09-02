package com.thetestpeople.trt.analysis

import org.joda.time.DateTime

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils._
import com.github.nscala_time.time.Imports._

/**
 * Analyse executions and produce test statuses over a series of points in time.
 *
 * @param sampleSize -- how many points in time to compute historical test statuses for.
 */
class HistoricalTestAnalyser(analysisConfiguration: AnalysisConfiguration, sampleSize: Int = 200) extends HasLogger {

  import HistoricalTestAnalyser._

  /**
   * Analyse the executions and produce test statuses over a series of points in time.
   *
   * @param executions - executions sorted by configuration, testId and executionTime.
   * @param executionIntervalsByConfig -- for each configuration, the interval between the first execution and
   *                                       last execution against that configuration.
   * @return map from configuration to historical test counts, ordered from earliest to latest
   */
  def analyseAll(
    executions: Iterator[ExecutionLite],
    executionIntervalsByConfig: Map[Configuration, Interval]): Map[Configuration, HistoricalTestCountsTimeline] = {

    val sampleTimesByConfig: Map[Configuration, List[DateTime]] = getSampleTimesByConfiguration(executionIntervalsByConfig)
    val testCountsBuilder = new TestCountsBuilder(sampleTimesByConfig)
    for (executionGroup ← new ExecutionGroupIterator(executions)) {
      val sampleTimes: List[DateTime] = sampleTimesByConfig(executionGroup.configuration)
      analyseExecutionGroup(executionGroup, testCountsBuilder, sampleTimes)
    }
    testCountsBuilder.build()
  }

  /**
   * @return for each configuration, a series of sample times across the execution interval for that configuration.
   *   The sample times are ordered most recent -> least recent.
   */
  private def getSampleTimesByConfiguration(executionIntervalsByConfig: Map[Configuration, Interval]): Map[Configuration, List[DateTime]] =
    for {
      (configuration, executionInterval) ← executionIntervalsByConfig
      sampleTimes = DateUtils.sampleTimesBetween(executionInterval, samples = sampleSize)
    } yield configuration -> sampleTimes.reverse

  /**
   * Compute all the test results for the given execution group and record them into the TestCountsBuilder.
   */
  private def analyseExecutionGroup(executionGroup: ExecutionGroup, testCountsBuilder: TestCountsBuilder, sampleTimes: List[DateTime]) {
    val quickTestAnalyser = new QuickTestAnalyser(executionGroup.executions.reverse.toArray)
    for (sampleTime ← sampleTimes) {
      quickTestAnalyser.ignoreExecutionsAfter(sampleTime)
      for (passFailBlock ← quickTestAnalyser.getMostRecentPassFailBlock) {
        val testStatus = TestAnalyser.calculateTestStatus(analysisConfiguration, passFailBlock)
        testCountsBuilder.recordResult(executionGroup.configuration, sampleTime, testStatus)
      }
    }
  }

}
