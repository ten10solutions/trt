package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.service.Clock

case class TestAnalysis(
  status: TestStatus,
  weather: Double,
  consecutiveFailures: Int,
  failingSinceOpt: Option[DateTime],
  lastPassedExecutionOpt: Option[Execution],
  lastFailedExecutionOpt: Option[Execution],
  whenAnalysed: DateTime,
  medianDurationOpt: Option[Duration],
  lastSummaryOpt: Option[String])
