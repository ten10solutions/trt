package com.thetestpeople.trt.model

import java.net.URI
import com.thetestpeople.trt.model.jenkins._
import org.joda.time.Duration

object SortBy {

  sealed trait Test {
    def descending: Boolean
  }

  object Test {
    case class Weather(descending: Boolean = false) extends SortBy.Test
    case class Group(descending: Boolean = false) extends SortBy.Test
    case class Name(descending: Boolean = false) extends SortBy.Test
    case class Duration(descending: Boolean = false) extends SortBy.Test
    case class ConsecutiveFailures(descending: Boolean = false) extends SortBy.Test
    case class StartedFailing(descending: Boolean = false) extends SortBy.Test
    case class LastPassed(descending: Boolean = false) extends SortBy.Test
    case class LastFailed(descending: Boolean = false) extends SortBy.Test
  }

}

trait Dao extends ExecutionDao with CiDao {

  /**
   * Run the given block within a transaction
   */
  def transaction[T](block: ⇒ T): T

  /**
   * @return test and analysis for this configuration. Returns None if there are no analysed results for the given test and configuration.
   */
  def getEnrichedTest(id: Id[Test], configuration: Configuration = Configuration.Default): Option[EnrichedTest]

  /**
   * @return the IDs of all tests in the DB (including deleted tests).
   */
  def getTestIds(): Seq[Id[Test]]

  /**
   * @return all tests marked as deleted
   */
  def getDeletedTests(): Seq[Test]

  /**
   * @return all test names matching the given pattern (case-insensitive, allows * wildcards)
   */
  def getTestNames(pattern: String): Seq[String]

  /**
   * @return all groups matching the given pattern (case-insensitive, allows * wildcards)
   */
  def getGroups(pattern: String): Seq[String]

  /**
   * Get tests and any analysis for the given configuration and filters.
   *
   * Excludes deleted tests
   *
   * @param nameOpt -- if Some(name), filter returned tests to those matching the given name (case insensitive, allows * wildcards)
   * @param groupOpt -- if Some(group), filter returned tests to those matching the given group (case insensitive, allows * wildcards)
   * @param testStatusOpt -- if Some(testStatus), filter returned tests to those matching the given status.
   */
  def getAnalysedTests(
    configuration: Configuration = Configuration.Default,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None,
    sortBy: SortBy.Test = SortBy.Test.Group()): Seq[EnrichedTest]

  /**
   * Gets test counts for all configurations.
   *
   * Excludes deleted tests
   *
   * @return a map of configuration to test counts for that configuration
   */
  def getTestCountsByConfiguration(): Map[Configuration, TestCounts]

  /**
   * Gets test counts for a given configuration (and filters).
   *
   * @param nameOpt -- if Some(name), filter returned tests to those matching the given name (case insensitive, allows * wildcards)
   * @param groupOpt -- if Some(group), filter returned tests to those matching the given group (case insensitive, allows * wildcards)
   *
   * Excludes deleted tests
   */
  def getTestCounts(
    configuration: Configuration = Configuration.Default,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None): TestCounts

  def getTestsById(testIds: Seq[Id[Test]]): Seq[Test]

  /**
   * Checks whether a test is already recorded for the given test's qualifiedName.
   * If not, a new Test record is persisted (the given ID is ignored).
   *
   * @return ID of either an existing or newly-added test.
   */
  def ensureTestIsRecorded(test: Test): Id[Test]

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true)

  def upsertAnalysis(analysis: Analysis)

  def getBatch(id: Id[Batch]): Option[BatchAndLog]

  /**
   * Return batches, ordered most recent first
   *
   * @param jobOpt -- if Some(job), then only return batches that were imported from the given job.
   * @param configurationOpt -- if Some(configuration), then only return batches that are associated with the given configuration
   * Otherwise, return all batches.
   */
  def getBatches(jobOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None): Seq[Batch]

  /**
   * Add a record for a new batch (the existing ID is ignored)
   *
   * @return ID of the newly added batch.
   */
  def newBatch(batch: Batch, logOpt: Option[String] = None): Id[Batch]

  def setBatchDuration(batchId: Id[Batch], durationOpt: Option[Duration]): Boolean

  /**
   * Delete the given batches and any associated data (executions, Jenkins import records, etc).
   *
   * This will also delete any tests that end up with no executions as a result of deleting the given batches.
   *
   * @return IDs of tests that haven't been deleted, but have had an associated execution deleted.
   */
  def deleteBatches(batchIds: Seq[Id[Batch]]): DeleteBatchResult

  def getSystemConfiguration(): SystemConfiguration

  def updateSystemConfiguration(newConfig: SystemConfiguration)

  def getConfigurations(): Seq[Configuration]

  /**
   * Return the configurations of executions that have been recorded of the given test.
   */
  def getConfigurations(testId: Id[Test]): Seq[Configuration]

  def setBatchComment(id: Id[Batch], text: String)

  def deleteBatchComment(id: Id[Batch])

  def setTestComment(id: Id[Test], text: String)

  def deleteTestComment(id: Id[Test])

  def getCategories(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[String]]

  def setCategories(testId: Id[Test], categories: Seq[String])

  def addCategories(testId: Id[Test], categories: Seq[String])

  def removeCategory(testId: Id[Test], category: String)

}

case class DeleteBatchResult(remainingTestIds: Seq[Id[Test]], deletedExecutionIds: Seq[Id[Execution]])