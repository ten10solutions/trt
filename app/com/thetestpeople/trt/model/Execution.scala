package com.thetestpeople.trt.model

import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * Record of the execution of a single test case.
 */
case class Execution(
  id: Id[Execution] = Id.dummy,
  batchId: Id[Batch],
  testId: Id[Test],
  /**
   * Time associated with the test execution, ideally the start time if available.
   */
  executionTime: DateTime,
  durationOpt: Option[Duration],
  summaryOpt: Option[String],
  passed: Boolean,
  configuration: Configuration) extends AbstractExecution with EntityType