package com.thetestpeople.trt.service

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.service.jenkins.CiServiceImpl
import com.thetestpeople.trt.analysis._
import java.net.URI
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.importer._
import com.thetestpeople.trt.importer.CiImportQueue
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
  protected val ciImportStatusManager: CiImportStatusManager,
  protected val batchRecorder: BatchRecorder,
  protected val ciImportQueue: CiImportQueue,
  protected val logIndexer: LogIndexer)
    extends Service with HasLogger with CiServiceImpl {

  import dao.transaction

  def getExecution(id: Id[Execution]): Option[EnrichedExecution] = transaction { dao.getEnrichedExecution(id) }

  def getExecutions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean], startingFrom: Int, limit: Int): ExecutionsAndTotalCount =
    transaction {
      val executions = dao.getEnrichedExecutions(configurationOpt, resultOpt, startingFrom, limit)
      val executionCount = dao.countExecutions(configurationOpt, resultOpt)
      ExecutionsAndTotalCount(executions, executionCount)
    }

  def getTestAndExecutions(id: Id[Test], configuration: Configuration, resultOpt: Option[Boolean] = None): Option[TestAndExecutions] = transaction {
    dao.getEnrichedTest(id, configuration) map { test ⇒
      val executions = dao.getEnrichedExecutionsForTest(id, Some(configuration), resultOpt)
      val otherConfigurations = dao.getConfigurations(id).filterNot(_ == configuration)
      val categories = dao.getCategories(Seq(id)).getOrElse(id, Seq()).map(_.category).sortBy(_.toLowerCase)
      val isIgnoredInConfiguration = dao.isTestIgnoredInConfiguration(id, configuration)
      TestAndExecutions(test, executions, otherConfigurations, categories, isIgnoredInConfiguration)
    }
  }

  def getTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    ignoredOpt: Option[Boolean] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    startingFrom: Int,
    limit: Int,
    sortBy: SortBy.Test = SortBy.Test.Group()): TestsInfo = transaction {

    val ignoredTests = dao.getIgnoredTests(configuration)
    val testCounts = dao.getTestCounts(
      configuration = configuration,
      nameOpt = nameOpt,
      groupOpt = groupOpt,
      categoryOpt = categoryOpt,
      ignoredTests = ignoredTests)

    val (blackListOpt, whiteListOpt) = ignoredOpt match {
      case None        ⇒ (None, None)
      case Some(true)  ⇒ (None, Some(ignoredTests))
      case Some(false) ⇒ (Some(ignoredTests), None)
    }

    val tests = dao.getAnalysedTests(
      configuration = configuration,
      testStatusOpt = testStatusOpt,
      nameOpt = nameOpt,
      groupOpt = groupOpt,
      categoryOpt = categoryOpt,
      blackListOpt = blackListOpt,
      whiteListOpt = whiteListOpt,
      startingFrom = startingFrom,
      limitOpt = Some(limit),
      sortBy = sortBy)

    TestsInfo(tests, testCounts, ignoredTests)
  }

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts] = transaction {
    val configAndCounts =
      for {
        configuration ← dao.getConfigurations
        ignoredTests = dao.getIgnoredTests(configuration)
        testCounts = dao.getTestCounts(configuration, ignoredTests = ignoredTests)
      } yield configuration -> testCounts
    configAndCounts.toMap
  }

  def getDeletedTests(): Seq[EnrichedTest] = transaction { dao.getDeletedTests().map(t ⇒ EnrichedTest(t)) }

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean) = transaction {
    if (deleted)
      logger.info("Marking tests as deleted: " + ids.mkString(", "))
    else
      logger.info("Marking tests as no longer deleted: " + ids.mkString(", "))
    dao.markTestsAsDeleted(ids, deleted)
    if (!deleted)
      analysisService.scheduleAnalysis(ids)
  }

  def addBatch(incomingBatch: Incoming.Batch): Id[Batch] =
    batchRecorder.recordBatch(incomingBatch).id

  def addExecutionsToBatch(batchId: Id[Batch], executions: Seq[Incoming.Execution]): Boolean =
    batchRecorder.recordExecutions(batchId, executions)

  def completeBatch(batchId: Id[Batch], durationOpt: Option[Duration]): Boolean = transaction {
    logger.info("sBatch completed: $batchId")
    dao.setBatchDuration(batchId, durationOpt)
  }

  def getBatchAndExecutions(id: Id[Batch], passedFilterOpt: Option[Boolean] = None): Option[EnrichedBatch] =
    transaction {
      dao.getBatch(id).map { batch ⇒
        val executions = dao.getEnrichedExecutionsInBatch(id, passedFilterOpt)
        batch.copy(executions = executions)
      }
    }

  def getBatches(jobOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean]): Seq[Batch] =
    transaction { dao.getBatches(jobOpt, configurationOpt, resultOpt) }

  def deleteBatches(batchIds: Seq[Id[Batch]]) = {
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

  def getAllHistoricalTestCounts: AllHistoricalTestCounts = analysisService.getAllHistoricalTestCounts

  def getHistoricalTestCounts(configuration: Configuration): Option[HistoricalTestCountsTimeline] =
    analysisService.getAllHistoricalTestCounts.getHistoricalTestCounts(configuration)

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

  def getCategoryNames(pattern: String): Seq[String] = transaction {
    val matches = dao.getCategoryNames("*" + pattern + "*").distinct
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

  def staleTests(configuration: Configuration): (Option[ExecutionTimeMAD], Seq[EnrichedTest]) = transaction {
    val analysedTests = dao.getAnalysedTests(configuration = configuration)
    new StaleTestCalculator().findStaleTests(analysedTests)
  }

  def setExecutionComment(id: Id[Execution], text: String): Boolean = transaction {
    if (dao.getEnrichedExecution(id).isEmpty)
      false
    else {
      logger.info(s"Updating comment for execution $id")
      text.trim match {
        case "" ⇒ dao.deleteExecutionComment(id)
        case s  ⇒ dao.setExecutionComment(id, text)
      }
      true
    }
  }

  def setBatchComment(id: Id[Batch], text: String): Boolean = transaction {
    if (dao.getBatch(id).isEmpty)
      false
    else {
      logger.info(s"Updating comment for batch $id")
      text.trim match {
        case "" ⇒ dao.deleteBatchComment(id)
        case s  ⇒ dao.setBatchComment(id, text)
      }
      true
    }
  }

  def setTestComment(id: Id[Test], text: String): Boolean = transaction {
    if (dao.getTestsById(Seq(id)).isEmpty)
      false
    else {
      logger.info(s"Updating comment for test $id")
      text.trim match {
        case "" ⇒ dao.deleteTestComment(id)
        case s  ⇒ dao.setTestComment(id, text)
      }
      true
    }
  }

  def addCategory(testId: Id[Test], category: String): AddCategoryResult = transaction {
    if (dao.getTestsById(Seq(testId)).isEmpty)
      AddCategoryResult.NoTestFound
    else {
      val existingCategories = dao.getCategories(Seq(testId)).getOrElse(testId, Seq())
      if (existingCategories contains category)
        AddCategoryResult.DuplicateCategory
      else {
        dao.addCategories(Seq(TestCategory(testId, category, isUserCategory = true)))
        logger.info(s"Added category $category to test $testId")
        AddCategoryResult.Success
      }
    }
  }

  def removeCategory(testId: Id[Test], category: String) = transaction {
    dao.removeCategories(testId, Seq(category))
    logger.info(s"Removed category $category from test $testId")
  }

  def ignoreTestsInConfiguration(testIds: Seq[Id[Test]], configuration: Configuration): Boolean = transaction {
    val ignoredConfigurationsByTest = dao.getIgnoredConfigurations(testIds)
    if (ignoredConfigurationsByTest.keySet == testIds.toSet) {
      val newIgnoredRecords =
        for {
          (testId, currentlyIgnoredConfigs) ← ignoredConfigurationsByTest.toSeq
          if !currentlyIgnoredConfigs.contains(configuration)
        } yield IgnoredTestConfiguration(testId, configuration)
      dao.addIgnoredTestConfigurations(newIgnoredRecords)
      val idsString = newIgnoredRecords.map(_.testId).mkString(", ")
      logger.info(s"Ignoring tests $idsString in configuration $configuration")
      true
    } else
      false
  }

  def unignoreTestsInConfiguration(testIds: Seq[Id[Test]], configuration: Configuration): Boolean = transaction {
    if (dao.getTestsById(testIds).size != testIds.size)
      false
    else {
      dao.removeIgnoredTestConfigurations(testIds, configuration)
      logger.info(s"Unignoring tests ${testIds.mkString(", ")} in configuration $configuration")
      true
    }
  }

}