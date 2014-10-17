package com.thetestpeople.trt.service

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.service.jenkins.JenkinsServiceImpl
import com.thetestpeople.trt.analysis._
import java.net.URI
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.jenkins.importer.JenkinsImportStatusManager
import com.thetestpeople.trt.jenkins.importer.CiImportQueue
import com.thetestpeople.trt.service.indexing.LogIndexer
import com.thetestpeople.trt.service.indexing.ExecutionHit
import com.thetestpeople.trt.service.indexing.SearchResult
import org.joda.time.DateTime
import org.joda.time.Duration

class ServiceImpl(
  protected val dao: Dao,
  protected val clock: Clock,
  protected val http: Http,
  protected val analysisService: AnalysisService,
  protected val jenkinsImportStatusManager: JenkinsImportStatusManager,
  protected val batchRecorder: BatchRecorder,
  protected val ciImportQueue: CiImportQueue,
  protected val logIndexer: LogIndexer)
    extends Service with HasLogger with JenkinsServiceImpl {

  import dao.transaction

  def getExecution(id: Id[Execution]): Option[EnrichedExecution] = transaction { dao.getEnrichedExecution(id) }

  def getExecutions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean], startingFrom: Int, limit: Int): ExecutionsAndTotalCount =
    transaction {
      val executions = dao.getEnrichedExecutions(configurationOpt, resultOpt, startingFrom, limit)
      val executionCount = dao.countExecutions(configurationOpt, resultOpt)
      ExecutionsAndTotalCount(executions, executionCount)
    }

  def getTestAndExecutions(id: Id[Test], configuration: Configuration, resultOpt: Option[Boolean] = None): Option[TestAndExecutions] = transaction {
    dao.getTestAndAnalysis(id, configuration) map { test ⇒
      val executions = dao.getEnrichedExecutionsForTest(id, Some(configuration), resultOpt)
      val otherConfigurations = dao.getConfigurations(id).filterNot(_ == configuration)
      TestAndExecutions(test, executions, otherConfigurations)
    }
  }

  def getTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int,
    limit: Int,
    sortBy: SortBy.Test = SortBy.Test.Group()): (TestCounts, Seq[TestAndAnalysis]) = transaction {

    val testCounts = dao.getTestCounts(
      configuration = configuration,
      nameOpt = nameOpt,
      groupOpt = groupOpt)
    val tests = dao.getAnalysedTests(
      configuration = configuration,
      testStatusOpt = testStatusOpt,
      nameOpt = nameOpt,
      groupOpt = groupOpt,
      startingFrom = startingFrom,
      limitOpt = Some(limit),
      sortBy = sortBy)

    (testCounts, tests)
  }

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts] = transaction { dao.getTestCountsByConfiguration() }

  def getDeletedTests(): Seq[TestAndAnalysis] = transaction { dao.getDeletedTests().map(t ⇒ TestAndAnalysis(t)) }

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean) = transaction {
    if (deleted)
      logger.info("Marking tests as deleted: " + ids.mkString(", "))
    else
      logger.info("Marking tests as no longer deleted: " + ids.mkString(", "))
    dao.markTestsAsDeleted(ids, deleted)
    if (!deleted)
      analysisService.scheduleAnalysis(ids)
  }

  def addBatch(incomingBatch: Incoming.Batch): Id[Batch] = transaction {
    batchRecorder.recordBatch(incomingBatch).id
  }

  def getBatchAndExecutions(id: Id[Batch], passedFilterOpt: Option[Boolean] = None): Option[BatchAndExecutions] =
    transaction {
      dao.getBatch(id).map {
        case BatchAndLog(batch, logOpt, importSpecIdOpt, commentOpt) ⇒
          val executions = dao.getEnrichedExecutionsInBatch(id, passedFilterOpt)
          BatchAndExecutions(batch, executions, logOpt, importSpecIdOpt, commentOpt)
      }
    }

  def getBatches(jobOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None): Seq[Batch] =
    transaction { dao.getBatches(jobOpt, configurationOpt) }

  def deleteBatches(batchIds: List[Id[Batch]]) = {
    val DeleteBatchResult(remainingTestIds, executionIds) = transaction {
      dao.deleteBatches(batchIds)
    }
    logger.info(s"Deleted batches ${batchIds.mkString(", ")}")
    logIndexer.deleteExecutions(executionIds)
    analysisService.scheduleAnalysis(remainingTestIds)
  }

  def getSystemConfiguration(): SystemConfiguration = transaction { dao.getSystemConfiguration() }

  def updateSystemConfiguration(newConfig: SystemConfiguration) = {
    val oldConfig = getSystemConfiguration()
    val analysisUpdateRequired = oldConfig.copy(projectNameOpt = None) != newConfig.copy(projectNameOpt = None)
    val testIds = transaction {
      dao.updateSystemConfiguration(newConfig)
      dao.getTestIds()
    }
    if (analysisUpdateRequired) {
      analysisService.scheduleAnalysis(testIds)
      analysisService.clearHistoricalTestCounts()
    }
    logger.info(s"Updated system configuration to $newConfig")
  }

  def getConfigurations(): Seq[Configuration] = transaction { dao.getConfigurations() }

  def getHistoricalTestCounts(): Map[Configuration, HistoricalTestCountsTimeline] =
    analysisService.getHistoricalTestCountsByConfig

  def getHistoricalTestCounts(configuration: Configuration): Option[HistoricalTestCountsTimeline] =
    analysisService.getHistoricalTestCountsByConfig.get(configuration)

  def analyseAllExecutions() {
    logger.info("Analysing all executions")
    analysisService.analyseAllExecutions()
  }

  def hasExecutions(): Boolean = transaction { dao.countExecutions() > 0 }

  def getTestNames(pattern: String): Seq[String] = transaction {
    val matches = dao.getTestNames("*" + pattern + "*").distinct
    if (pattern.contains("*"))
      matches.sorted
    else
      helpfullySort(pattern, matches)
  }

  def getGroups(pattern: String): Seq[String] = transaction {
    val matches = dao.getGroups("*" + pattern + "*").distinct
    if (pattern.contains("*"))
      matches.sorted
    else
      helpfullySort(pattern, matches)
  }

  /**
   * Sort autocomplete suggestions in a hopefully-useful way: prefix matches first, then infix matches.
   */
  private def helpfullySort(pattern: String, matches: Seq[String]): Seq[String] = {
    val (prefixes, infixes) = matches.partition(_ startsWith pattern)
    prefixes.sorted ++ infixes.sorted
  }

  def searchLogs(query: String, startingFrom: Int = 0, limit: Int = Integer.MAX_VALUE): (Seq[ExecutionAndFragment], Int) = {
    val SearchResult(executionHits, total) = logIndexer.searchExecutions(query, startingFrom, limit)
    val executions = transaction { dao.getEnrichedExecutions(executionHits.map(_.executionId)) }
    val executionAndFragments =
      for {
        execution ← executions
        ExecutionHit(executionId, fragment) ← executionHits
        if execution.id == executionId
      } yield ExecutionAndFragment(execution, fragment)
    (executionAndFragments, total)
  }

  def getExecutionVolume(configurationOpt: Option[Configuration]): Option[ExecutionVolume] =
    analysisService.getExecutionVolume(configurationOpt)

  def staleTests(configuration: Configuration): (Option[ExecutionTimeMAD], Seq[TestAndAnalysis]) = transaction {
    val analysedTests = dao.getAnalysedTests(configuration = configuration)
    new StaleTestCalculator().findStaleTests(analysedTests)
  }

  def setExecutionComment(id: Id[Execution], text: String): Boolean =
    if (transaction(dao.getEnrichedExecution(id)).isEmpty)
      false
    else {
      logger.info(s"Updating comment for execution $id")
      text.trim match {
        case "" ⇒ transaction { dao.deleteExecutionComment(id) }
        case s  ⇒ transaction { dao.setExecutionComment(id, text) }
      }
      true
    }

  def setBatchComment(id: Id[Batch], text: String): Boolean =
    if (transaction(dao.getBatch(id)).isEmpty)
      false
    else {
      logger.info(s"Updating comment for batch $id")
      text.trim match {
        case "" ⇒ transaction { dao.deleteBatchComment(id) }
        case s  ⇒ transaction { dao.setBatchComment(id, text) }
      }
      true
    }

  def setTestComment(id: Id[Test], text: String): Boolean =
    if (transaction(dao.getTestsById(Seq(id))).isEmpty)
      false
    else {
      logger.info(s"Updating comment for test $id")
      text.trim match {
        case "" ⇒ transaction { dao.deleteTestComment(id) }
        case s  ⇒ transaction { dao.setTestComment(id, text) }
      }
      true
    }

}