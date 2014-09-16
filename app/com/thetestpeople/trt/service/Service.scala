package com.thetestpeople.trt.service

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service.jenkins.JenkinsService
import com.thetestpeople.trt.analysis.HistoricalTestCounts
import org.joda.time.Duration
import java.net.URI
import com.thetestpeople.trt.analysis.HistoricalTestCountsTimeline

case class TestAndExecutions(test: TestAndAnalysis, executions: List[EnrichedExecution], otherConfigurations: Seq[Configuration])

case class BatchAndExecutions(batch: Batch, executions: List[EnrichedExecution], logOpt: Option[String])

case class ExecutionsAndTotalCount(executions: List[EnrichedExecution], total: Int)

trait Service extends JenkinsService {

  def addBatch(batch: Incoming.Batch): Id[Batch]

  def getBatchAndExecutions(id: Id[Batch], passedFilterOpt: Option[Boolean] = None): Option[BatchAndExecutions]

  /**
   * Return batches, ordered most recent first
   */
  def getBatches(jobOpt: Option[Id[JenkinsJob]] = None, configurationOpt: Option[Configuration] = None): Seq[Batch]

  def deleteBatches(batchIds: List[Id[Batch]])

  def getTestAndExecutions(id: Id[Test], configuration: Configuration = Configuration.Default): Option[TestAndExecutions]

  def getTests(
    configuration: Configuration = Configuration.Default,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int = 0,
    limit: Int = Integer.MAX_VALUE): (TestCounts, Seq[TestAndAnalysis])

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts]

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true)

  def getHistoricalTestCounts(): Map[Configuration, HistoricalTestCountsTimeline]

  def getHistoricalTestCounts(configuration: Configuration): Option[HistoricalTestCountsTimeline]

  def recomputeHistoricalTestCounts()

  def getExecutions(configurationOpt: Option[Configuration], startingFrom: Int, limit: Int): ExecutionsAndTotalCount

  def getExecution(id: Id[Execution]): Option[EnrichedExecution]

  def getSystemConfiguration(): SystemConfiguration

  def updateSystemConfiguration(newConfig: SystemConfiguration)

  def getConfigurations(): Seq[Configuration]

  /**
   * Return true iff there is at least one execution recorded
   */
  def hasExecutions(): Boolean

  def getTestNames(pattern: String): Seq[String]
  
}