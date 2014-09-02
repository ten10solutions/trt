package com.thetestpeople.trt.model

import org.joda.time.DateTime

/**
 * Bare-bones execution record used for computing pass/fail counts quickly.
 */
case class ExecutionLite(
  configuration: Configuration,
  testId: Id[Test],
  executionTime: DateTime,
  passed: Boolean)