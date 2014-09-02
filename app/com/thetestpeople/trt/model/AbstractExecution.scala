package com.thetestpeople.trt.model

import org.joda.time.DateTime
import org.joda.time.Duration
import com.github.nscala_time.time.Imports._

trait AbstractExecution {

  def id: Id[_]

  /**
   * Time the test completed executing
   */
  def executionTime: DateTime

  def durationOpt: Option[Duration]

  def passed: Boolean

  def failed: Boolean = !passed

}