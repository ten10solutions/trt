package com.thetestpeople.trt.analysis

import org.joda.time.DateTime

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils._
import com.github.nscala_time.time.Imports._

/**
 * Analyse executions and produce test statuses over a series of points in time.
 *
 * @param executionIntervalsByConfig -- for each configuration, the interval between the first execution and
 *                                      last execution against that configuration.
 */
class HistoricalTestAnalyser(
  executionIntervalsByConfig: Map[Configuration, Interval],
  analysisConfiguration: AnalysisConfiguration,
  timeZone: DateTimeZone = DateTimeZone.getDefault)
    extends ExecutionAnalyser[Map[Configuration, HistoricalTestCountsTimeline]] with HasLogger {

  private val sampleTimesByConfig: Map[Configuration, Seq[DateTime]] = getSampleTimesByConfiguration(executionIntervalsByConfig)
  private val testCountsBuilder = new TestCountsBuilder(sampleTimesByConfig)

  /**
   * Analyse the executions and produce test statuses over a series of points in time.
   */
  def executionGroup(executionGroup: ExecutionGroup) {
    val sampleTimes: Seq[DateTime] = sampleTimesByConfig(executionGroup.configuration)
    analyseExecutionGroup(executionGroup, testCountsBuilder, sampleTimes)
  }

  /**
   * @return map from configuration to historical test counts, ordered from earliest to latest
   */
  def finalise(): Map[Configuration, HistoricalTestCountsTimeline] = testCountsBuilder.build()

  /**
   * @return for each configuration, a series of sample times across the execution interval for that configuration.
   *   The sample times are ordered most recent -> least recent.
   */
  private def getSampleTimesByConfiguration(executionIntervalsByConfig: Map[Configuration, Interval]): Map[Configuration, Seq[DateTime]] =
    for {
      (configuration, executionInterval) ← executionIntervalsByConfig
      sampleTimes = DateUtils.getAllDaysIn(executionInterval, timeZone)
    } yield configuration -> sampleTimes.reverse

  /**
   * Compute all the test results for the given execution group and record them into the TestCountsBuilder.
   */
  private def analyseExecutionGroup(executionGroup: ExecutionGroup, testCountsBuilder: TestCountsBuilder, sampleTimes: Seq[DateTime]) {
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
