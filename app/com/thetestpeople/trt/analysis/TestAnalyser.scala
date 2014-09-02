package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.service.Clock

object TestAnalyser {

  def calculateTestStatus(config: AnalysisConfiguration, block: PassFailBlock): TestStatus =
    if (block.passed) {
      val passedLongEnough = block.duration >= config.passDurationThreshold
      val passedOftenEnough = block.count >= config.passCountThreshold
      if (passedLongEnough && passedOftenEnough)
        TestStatus.Pass
      else
        TestStatus.Warn
    } else {
      val failingTooLong = block.duration >= config.failureDurationThreshold
      val failedTooOften = block.count >= config.failureCountThreshold
      if (failingTooLong && failedTooOften)
        TestStatus.Fail
      else
        TestStatus.Warn
    }

}

class TestAnalyser(clock: Clock, config: AnalysisConfiguration) {

  import config._

  def analyse(executions: List[Execution]): Option[TestAnalysis] = {
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
      TestAnalysis(
        status = status,
        weather = weather,
        consecutiveFailures = consecutiveFailures,
        failingSinceOpt = failingSinceOpt,
        lastPassedExecutionOpt = lastPassedExecutionOpt,
        lastFailedExecutionOpt = lastFailedExecutionOpt,
        whenAnalysed = clock.now)
    }
  }

  private def calculateWeather(sortedExecutions: List[Execution]): Double = {
    val recentExecutions = sortedExecutions.take(10)
    recentExecutions.count(_.passed).toDouble / recentExecutions.size
  }

  private def duration(recentPasses: List[Execution]): Duration =
    (recentPasses.last.executionTime to recentPasses.head.executionTime).duration

  private def calculateTestStatus(recentFailures: List[Execution], recentPasses: List[Execution]): TestStatus =
    TestAnalyser.calculateTestStatus(config,
      if (recentFailures.isEmpty)
        PassFailBlock(recentPasses.size, true, duration(recentPasses))
      else
        PassFailBlock(recentFailures.size, false, duration(recentFailures)))

}