package com.thetestpeople.trt.model

import org.joda.time.Duration

trait AnalysisConfiguration {

  def failureDurationThreshold: Duration

  def failureCountThreshold: Int

  def passDurationThreshold: Duration

  def passCountThreshold: Int

}