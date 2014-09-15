package com.thetestpeople.trt.service

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.service.jenkins.JenkinsServiceImpl
import com.thetestpeople.trt.analysis._
import java.net.URI
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.jenkins.importer.JenkinsImportStatusManager
import com.thetestpeople.trt.jenkins.importer.JenkinsImportQueue

class ServiceImpl(
  protected val dao: Dao,
  protected val clock: Clock,
  protected val http: Http,
  protected val analysisService: AnalysisService,
  protected val jenkinsImportStatusManager: JenkinsImportStatusManager,
  protected val batchRecorder: BatchRecorder,
  protected val jenkinsImportQueue: JenkinsImportQueue)
    extends Service with HasLogger with JenkinsServiceImpl {

  import dao.transaction

  def getExecution(id: Id[Execution]): Option[EnrichedExecution] = transaction { dao.getEnrichedExecution(id) }

  def getExecutions(configurationOpt: Option[Configuration], startingFrom: Int, limit: Int): ExecutionsAndTotalCount =
    transaction {
      val executions = dao.getEnrichedExecutions(configurationOpt, startingFrom, limit)
      val executionCount = dao.countExecutions(configurationOpt)
      ExecutionsAndTotalCount(executions, executionCount)
    }

  def getTestAndExecutions(id: Id[Test], configuration: Configuration): Option[TestAndExecutions] = transaction {
    dao.getTestAndAnalysis(id, configuration) map { test ⇒
      val executions = dao.getEnrichedExecutionsForTest(id, Some(configuration))
      TestAndExecutions(test, executions)
    }
  }

  def getTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int,
    limit: Int): (TestCounts, List[TestAndAnalysis]) = transaction {

    val testCounts = dao.getTestCounts(
      configuration = configuration,
      groupOpt = groupOpt)
    val tests = dao.getAnalysedTests(
      configuration = configuration,
      testStatusOpt = testStatusOpt,
      groupOpt = groupOpt,
      startingFrom = startingFrom,
      limitOpt = Some(limit))

    (testCounts, tests)
  }

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts] = transaction { dao.getTestCountsByConfiguration() }

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean) = transaction { dao.markTestsAsDeleted(ids, deleted) }

  def addBatch(incomingBatch: Incoming.Batch): Id[Batch] = transaction {
    batchRecorder.recordBatch(incomingBatch).id
  }

  def getBatchAndExecutions(id: Id[Batch], passedFilterOpt: Option[Boolean] = None): Option[BatchAndExecutions] =
    transaction {
      dao.getBatch(id).map {
        case BatchAndLog(batch, logOpt) ⇒
          val executions = dao.getEnrichedExecutionsInBatch(id, passedFilterOpt)
          BatchAndExecutions(batch, executions, logOpt)
      }
    }

  def getBatches(jobOpt: Option[Id[JenkinsJob]] = None, configurationOpt: Option[Configuration] = None): List[Batch] =
    transaction { dao.getBatches(jobOpt, configurationOpt) }

  def deleteBatches(batchIds: List[Id[Batch]]) = {
    val testIds = transaction {
      dao.deleteBatches(batchIds)
    }
    logger.info(s"Deleted batches ${batchIds.mkString(", ")}")
    analysisService.scheduleAnalysis(testIds)
  }

  def getSystemConfiguration(): SystemConfiguration = transaction { dao.getSystemConfiguration() }

  def updateSystemConfiguration(newConfig: SystemConfiguration) = {
    val testIds = transaction {
      dao.updateSystemConfiguration(newConfig)
      dao.getTestIds()
    }
    logger.info(s"Updated system configuration to $newConfig")
    analysisService.scheduleAnalysis(testIds)
    analysisService.clearHistoricalTestCounts()
  }

  def getConfigurations(): List[Configuration] = transaction { dao.getConfigurations() }

  def getHistoricalTestCounts(): Map[Configuration, HistoricalTestCountsTimeline] =
    analysisService.getHistoricalTestCountsByConfig

  def getHistoricalTestCounts(configuration: Configuration): Option[HistoricalTestCountsTimeline] =
    analysisService.getHistoricalTestCountsByConfig.get(configuration)

  def recomputeHistoricalTestCounts() {
    logger.info("Computing historical test counts")
    analysisService.recomputeHistoricalTestCounts()
  }

  def hasExecutions(): Boolean = transaction { dao.countExecutions() > 0 }

}