package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins._
import org.joda.time._

trait ExecutionDao {

  /**
   * Iterate over all executions. Results are sorted by configuration, testId and executionTime.
   *
   * Executions of deleted tests are excluded.
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
   * @return executions matching given ids. No execution logs will be included.
   */
  def getEnrichedExecutions(ids: Seq[Id[Execution]]): Seq[EnrichedExecution]
  
  /**
   * @param configurationOpt -- if defined, filter executions to those against the given configuration
   * @param resultOpt -- if Some(true), only return passing executions; if Some(false), failing executions; else either 
   * @return executions, ordered most recent first, then by test group, then by test name. No execution logs will be provided.
   */
  def getEnrichedExecutions(configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean] = None, startingFrom: Int = 0, limit: Int = Integer.MAX_VALUE): Seq[EnrichedExecution]

  /**
   * @param configurationOpt -- if defined, restrict executions to only those against the given configuration
   * @return executions of the given test, ordered most recent first. No execution logs will be provided.
   */
  def getEnrichedExecutionsForTest(id: Id[Test], configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean] = None): Seq[EnrichedExecution]

  /**
   * @param passedFilterOpt -- if Some(true), return executions that passed. If Some(false), return only executions
   * that failed. If None, return all executions.
   * @return executions in the given batch. No execution logs will be provided.
   */
  def getEnrichedExecutionsInBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean] = None): Seq[EnrichedExecution]

  /**
   * @param configurationOpt -- if defined, only count executions against the given configuration.
    * @param resultOpt -- if Some(true), only return passing executions; if Some(false), failing executions; else either 
   * @return count of the executions recorded
   */
  def countExecutions(configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean] = None): Int

  /**
   * Add a record for a new test execution.
   *
   * Any id in the given execution is ignored.
   *
   * @return id of the newly added execution.
   */
  def newExecution(execution: Execution, logOpt: Option[String] = None): Id[Execution]

  def setExecutionComment(id: Id[Execution], text: String)
  
  def deleteExecutionComment(id: Id[Execution])
  
}