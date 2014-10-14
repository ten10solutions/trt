package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.service.Clock
import com.thetestpeople.trt.utils.StatsUtils
import org.joda.time.Duration

object TestAnalyser {

  def calculateTestStatus(config: AnalysisConfiguration, block: PassFailBlock): TestStatus =
    if (block.passed) {
      val passedLongEnough = block.duration >= config.passDurationThreshold
      val passedOftenEnough = block.count >= config.passCountThreshold
      if (passedLongEnough && passedOftenEnough)
        TestStatus.Healthy
      else
        TestStatus.Warning
    } else {
      val failingTooLong = block.duration >= config.failureDurationThreshold
      val failedTooOften = block.count >= config.failureCountThreshold
      if (failingTooLong && failedTooOften)
        TestStatus.Broken
      else
        TestStatus.Warning
    }

}

class TestAnalyser(clock: Clock, config: AnalysisConfiguration) {

  import config._

  def analyse(executions: Seq[Execution]): Option[TestAnalysis] = {
    val sortedExecutions = executions.sortBy(_.executionTime).reverse
    sortedExecutions.headOption map { mostRecentExecution ⇒
      val lastPassedExecutionOpt = sortedExecutions.find(_.passed)
      val lastFailedExecutionOpt = sortedExecutions.find(_.failed)
      val recentFailures = sortedExecutions.takeWhile(_.failed)
      val recentPasses = sortedExecutions.takeWhile(_.passed)
      val consecutiveFailures = recentFailures.size
      val failingSinceOpt = recentFailures.lastOption.map(_.executionTime)
      val status = calculateTestStatus(recentFailures, recentPasses)
      val weather = calculateWeather(sortedExecutions)
      val medianDurationOpt = medianDuration(sortedExecutions)
      TestAnalysis(
        status = status,
        weather = weather,
        consecutiveFailures = consecutiveFailures,
        failingSinceOpt = failingSinceOpt,
        lastPassedExecutionOpt = lastPassedExecutionOpt,
        lastFailedExecutionOpt = lastFailedExecutionOpt,
        whenAnalysed = clock.now,
        medianDurationOpt = medianDurationOpt)
    }
  }

  private def medianDuration(executions: Seq[Execution]): Option[Duration] = {
    val durations = executions.flatMap(_.durationOpt).map(_.getMillis.toDouble)
    StatsUtils.median(durations).map(m => Duration.millis(m.longValue))
  }
  
  private def calculateWeather(sortedExecutions: Seq[Execution]): Double = {
    val recentExecutions = sortedExecutions.take(10)
    recentExecutions.count(_.passed).toDouble / recentExecutions.size
  }

  private def duration(recentPasses: Seq[Execution]): Duration =
    (recentPasses.last.executionTime to recentPasses.head.executionTime).duration

  private def calculateTestStatus(recentFailures: Seq[Execution], recentPasses: Seq[Execution]): TestStatus =
    TestAnalyser.calculateTestStatus(config,
      if (recentFailures.isEmpty)
        PassFailBlock(recentPasses.size, true, duration(recentPasses))
      else
        PassFailBlock(recentFailures.size, false, duration(recentFailures)))

}