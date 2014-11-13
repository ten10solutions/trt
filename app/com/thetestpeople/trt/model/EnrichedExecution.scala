package com.thetestpeople.trt.model

import org.joda.time.DateTime
import org.joda.time.Duration

/**
 * A test execution enriched with related data.
 *
 * Note: as an optimisation, logOpt can be None, even when there is a log for the execution, depending on how
 * the data was retrieved.
 */
case class EnrichedExecution(
    execution: Execution,
    qualifiedName: QualifiedName,
    batchNameOpt: Option[String],
    logOpt: Option[String],
    commentOpt: Option[String]) {

  def id: Id[Execution] = execution.id

  def batchId: Id[Batch] = execution.batchId

  def testId: Id[Test] = execution.testId

  def summaryOpt: Option[String] = execution.summaryOpt

  /**
   * Time associated with the test execution, ideally the start time if available.
   */
  def executionTime: DateTime = execution.executionTime

  def durationOpt: Option[Duration] = execution.durationOpt

  def passed: Boolean = execution.passed

  def failed: Boolean = !passed

  def configuration: Configuration = execution.configuration

}
