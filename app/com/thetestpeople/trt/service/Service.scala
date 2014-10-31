package com.thetestpeople.trt.service

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service.jenkins.CiService
import com.thetestpeople.trt.analysis.HistoricalTestCounts
import org.joda.time.Duration
import java.net.URI
import com.thetestpeople.trt.analysis.HistoricalTestCountsTimeline
import com.thetestpeople.trt.analysis.ExecutionVolume
import com.thetestpeople.trt.analysis.ExecutionTimeMAD

case class TestAndExecutions(
  test: EnrichedTest,
  executions: Seq[EnrichedExecution],
  otherConfigurations: Seq[Configuration],
  categories: Seq[String] = Seq())

case class BatchAndExecutions(
  batch: Batch,
  executions: Seq[EnrichedExecution],
  logOpt: Option[String],
  importSpecIdOpt: Option[Id[CiImportSpec]],
  commentOpt: Option[String])

case class ExecutionsAndTotalCount(executions: Seq[EnrichedExecution], total: Int)

case class ExecutionAndFragment(execution: EnrichedExecution, fragment: String)

sealed trait AddCategoryResult
object AddCategoryResult {
  case object Success extends AddCategoryResult
  case object NoTestFound extends AddCategoryResult
  case object DuplicateCategory extends AddCategoryResult
}

trait Service extends CiService {

  def addBatch(batch: Incoming.Batch): Id[Batch]

  def addExecutionsToBatch(batchId: Id[Batch], executions: Seq[Incoming.Execution]): Boolean

  def completeExecution(batchId: Id[Batch], durationOpt: Option[Duration]): Boolean

  def getBatchAndExecutions(id: Id[Batch], passedFilterOpt: Option[Boolean] = None): Option[BatchAndExecutions]

  /**
   * Return batches, ordered most recent first
   */
  def getBatches(jobOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None): Seq[Batch]

  def deleteBatches(batchIds: List[Id[Batch]])

  def getTestAndExecutions(id: Id[Test], configuration: Configuration = Configuration.Default, resultOpt: Option[Boolean] = None): Option[TestAndExecutions]

  def getTests(
    configuration: Configuration = Configuration.Default,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    startingFrom: Int = 0,
    limit: Int = Integer.MAX_VALUE,
    sortBy: SortBy.Test = SortBy.Test.Group()): (TestCounts, Seq[EnrichedTest])

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts]

  /**
   * @return tests that have been marked as deleted. No analysis or comment is retrieved.
   */
  def getDeletedTests(): Seq[EnrichedTest]

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true)

  def getHistoricalTestCounts(): Map[Configuration, HistoricalTestCountsTimeline]

  def getHistoricalTestCounts(configuration: Configuration): Option[HistoricalTestCountsTimeline]

  def analyseAllExecutions()

  def getExecutions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean], startingFrom: Int, limit: Int): ExecutionsAndTotalCount

  def getExecution(id: Id[Execution]): Option[EnrichedExecution]

  def getSystemConfiguration(): SystemConfiguration

  def updateSystemConfiguration(newConfig: SystemConfiguration)

  def getConfigurations(): Seq[Configuration]

  /**
   * Return true iff there is at least one execution recorded
   */
  def hasExecutions(): Boolean

  def getTestNames(pattern: String): Seq[String]

  def getGroups(pattern: String): Seq[String]

  def getCategoryNames(pattern: String): Seq[String]
  
  def searchLogs(query: String, startingFrom: Int = 0, limit: Int = Integer.MAX_VALUE): (Seq[ExecutionAndFragment], Int)

  def getExecutionVolume(configurationOpt: Option[Configuration]): Option[ExecutionVolume]

  def staleTests(configuration: Configuration): (Option[ExecutionTimeMAD], Seq[EnrichedTest])

  /**
   * @return true iff an execution with the given id was present in the DB
   */
  def setExecutionComment(id: Id[Execution], text: String): Boolean

  /**
   * @return true iff a batch with the given id was present in the DB
   */
  def setBatchComment(id: Id[Batch], text: String): Boolean

  /**
   * @return true iff a batch with the given id was present in the DB
   */
  def setTestComment(id: Id[Test], text: String): Boolean

  def addCategory(testId: Id[Test], category: String): AddCategoryResult

  def removeCategory(testId: Id[Test], category: String)

}