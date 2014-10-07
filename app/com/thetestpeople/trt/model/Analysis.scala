package com.thetestpeople.trt.model

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._

case class Analysis(
    testId: Id[Test],
    configuration: Configuration,
    status: TestStatus,
    weather: Double,
    consecutiveFailures: Int,
    failingSinceOpt: Option[DateTime],
    lastPassedExecutionIdOpt: Option[Id[Execution]],
    lastPassedTimeOpt: Option[DateTime],
    lastFailedExecutionIdOpt: Option[Id[Execution]],
    lastFailedTimeOpt: Option[DateTime],
    whenAnalysed: DateTime,
    medianDurationOpt: Option[Duration]) extends EntityType {

  private lazy val lastPassed: Boolean =
    (lastPassedTimeOpt, lastFailedTimeOpt) match {
      case (None, None)                         ⇒ throw new RuntimeException("Analysis does not have a pass or fail time")
      case (Some(_), None)                      ⇒ true
      case (None, Some(_))                      ⇒ false
      case (Some(passedTime), Some(failedTime)) ⇒ passedTime >= failedTime
    }

  def lastExecutionId: Id[Execution] = if (lastPassed) lastPassedExecutionIdOpt.get else lastFailedExecutionIdOpt.get

  def lastExecutionTime: DateTime = if (lastPassed) lastPassedTimeOpt.get else lastFailedTimeOpt.get

}