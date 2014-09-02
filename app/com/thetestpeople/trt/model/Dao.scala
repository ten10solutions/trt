package com.thetestpeople.trt.model

import java.net.URI
import com.thetestpeople.trt.model.jenkins._

trait Dao extends ExecutionDao with JenkinsDao {

  /**
   * Run the given block within a transaction
   */
  def transaction[T](block: ⇒ T): T

  def getTestAndAnalysis(id: Id[Test], configuration: Configuration = Configuration.Default): Option[TestAndAnalysis]

  /**
   * @return the IDs of all tests in the DB
   */
  def getTestIds(): List[Id[Test]]

  /**
   * @param groupOpt -- if Some(group), filter returned tests to those matching the given group.
   * @param testStatusOpt -- if Some(testStatus), filter returned tests to those matching the given status.
   */
  def getAnalysedTests(
    configuration: Configuration = Configuration.Default,
    testStatusOpt: Option[TestStatus] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None): List[TestAndAnalysis]

  /**
   * @return a map of configuration to test counts for that configuration
   */
  def getTestCountsByConfiguration(): Map[Configuration, TestCounts]

  def getTestCounts(
    configuration: Configuration = Configuration.Default,
    groupOpt: Option[String] = None): TestCounts

  def getTestsById(testIds: List[Id[Test]]): List[Test]

  /**
   * Checks whether a test is already recorded for the given test's qualifiedName.
   * If not, a new Test record is persisted (the given ID is ignored).
   *
   * @returns ID of either an existing or newly-added test.
   */
  def ensureTestIsRecorded(test: Test): Id[Test]

  def upsertAnalysis(analysis: Analysis)

  def getBatch(id: Id[Batch]): Option[BatchAndLog]

  /**
   * Return batches, ordered most recent first
   *
   * @param jobOpt -- if Some(job), then only return batches that were imported from the given job.
   * Otherwise, return all batches.
   */
  def getBatches(jobOpt: Option[Id[JenkinsJob]] = None): List[Batch]

  /**
   * Add a record for a new batch (the existing ID is ignored)
   *
   * @returns ID of the newly added batch.
   */
  def newBatch(batch: Batch, logOpt: Option[String] = None): Id[Batch]

  /**
   * Delete the given batches and any associated data (executions, Jenkins import records, etc).
   *
   * This will also delete any tests that end up with no executions as a result of deleting the given batches.
   *
   * @return IDs of tests that haven't been deleted, but have had an associated execution deleted.
   */
  def deleteBatches(batchIds: List[Id[Batch]]): List[Id[Test]]

  def getSystemConfiguration(): SystemConfiguration

  def updateSystemConfiguration(newConfig: SystemConfiguration)

  def getConfigurations(): List[Configuration]

}