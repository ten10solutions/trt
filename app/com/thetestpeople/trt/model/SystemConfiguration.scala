package com.thetestpeople.trt.model

import org.joda.time.Duration

import com.github.nscala_time.time.Imports._

case class SystemConfiguration(
  projectNameOpt: Option[String] = None,
  failureDurationThreshold: Duration = 6.hours,
  failureCountThreshold: Int = 3,
  passDurationThreshold: Duration = 0.hours,
  passCountThreshold: Int = 1) extends AnalysisConfiguration