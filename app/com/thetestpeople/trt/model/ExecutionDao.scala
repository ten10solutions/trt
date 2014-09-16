package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins._
import org.joda.time._

trait ExecutionDao {

  /**
   * Allow iteration over all executions of non-deleted tests. Results are sorted by configuration, testId and executionTime.
   *
   * Once this method is complete, it will close any resources used.
   *
   * @param a function to process the executions
   */
  def iterateAllExecutions[T](f: Iterator[ExecutionLite] ⇒ T): T

  /**
   * @return a map from configuration to the entire interval over which executions have occurred (that is, the interval
   *  from the earliest execution recorded against that configuration, to the most recent execution).
   */
  def getExecutionIntervalsByConfig(): Map[Configuration, Interval]

  /**
   * @return executions of the given test, ordered most recent first
   */
  def getExecutionsForTest(id: Id[Test]): Seq[Execution]

  def getExecutionLog(id: Id[Execution]): Option[String]

  /**
   * @return execution with additional related data. It will also supply the execution log, if one exists.
   */
  def getEnrichedExecution(id: Id[Execution]): Option[EnrichedExecution]

  /**
   * @param configurationOpt -- if defined, filter executions to those against the given configuration
   * @return executions, ordered most recent first. No execution logs will be provided.
   */
  def getEnrichedExecutions(configurationOpt: Option[Configuration] = None, startingFrom: Int = 0, limit: Int = Integer.MAX_VALUE): Seq[EnrichedExecution]

  /**
   * @param configurationOpt -- if defined, restrict executions to only those against the given configuration
   * @return executions of the given test, ordered most recent first. No execution logs will be provided.
   */
  def getEnrichedExecutionsForTest(id: Id[Test], configurationOpt: Option[Configuration] = None): Seq[EnrichedExecution]

  /**
   * @param passedFilterOpt -- if Some(true), return executions that passed. If Some(false), return only executions
   * that failed. If None, return all executions.
   * @return executions in the given batch. No execution logs will be provided.
   */
  def getEnrichedExecutionsInBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean] = None): Seq[EnrichedExecution]

  /**
   * @param configurationOpt -- if defined, only count executions against the given configuration.
   * @return count of the executions recorded
   */
  def countExecutions(configurationOpt: Option[Configuration] = None): Int

  /**
   * Add a record for a new test execution.
   *
   * Any id in the given execution is ignored.
   *
   * @returns id of the newly added execution.
   */
  def newExecution(execution: Execution, logOpt: Option[String] = None): Id[Execution]

}