package com.thetestpeople.trt.service

import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.service.indexing.LogIndexer
import org.joda.time.Duration
import scala.PartialFunction.condOpt
import com.github.nscala_time.time.Imports._

class BatchRecorder(dao: Dao, clock: Clock, analysisService: AnalysisService, logIndexer: LogIndexer) extends HasLogger {

  def recordBatch(incomingBatch: Incoming.Batch): Batch = Utils.time("BatchRecorder.recordBatch") {
    val affectedEntities = dao.transaction {
      val batch = recordBatchOnly(incomingBatch)
      recordExecutions(batch, incomingBatch.executions)
    }
    handleAffectedEntities(affectedEntities)
    logger.info(s"New batch recorded, name = ${incomingBatch.nameOpt}, id = ${affectedEntities.batch.id}, executions = ${incomingBatch.executions.size}")
    affectedEntities.batch
  }

  /**
   * @return true iff a batch with the given batchId was found
   */
  def recordExecutions(batchId: Id[Batch], executions: Seq[Incoming.Execution]): Boolean = {
    val affectedEntities = dao.transaction {
      val batch = dao.getBatch(batchId).getOrElse(return false).batch
      recordExecutions(batch, executions)
    }
    handleAffectedEntities(affectedEntities)
    updateBatchSummaryStats(affectedEntities)
    logger.info(s"Added ${executions.size} to batch ${affectedEntities.batch.nameOpt}")
    true
  }

  private def updateBatchSummaryStats(affectedEntities: AffectedEntities) {
    val AffectedEntities(batch, executions, testIds) = affectedEntities
    val updatedBatch = batch.copy(
      passed = batch.passed && executions.forall(_.passed),
      passCount = batch.passCount + executions.count(_.passed),
      failCount = batch.failCount + executions.count(_.failed),
      totalCount = batch.totalCount + executions.size)
    dao.updateBatch(updatedBatch)
  }

  private case class AffectedEntities(batch: Batch, executions: Seq[EnrichedExecution], testIds: Seq[Id[Test]])

  private def handleAffectedEntities(affectedEntities: AffectedEntities) {
    analysisService.scheduleAnalysis(affectedEntities.testIds)
    logIndexer.addExecutions(affectedEntities.executions)
  }

  private def recordExecutions(batch: Batch, incomingExecutions: Seq[Incoming.Execution]): AffectedEntities = {
    def recordExecution(execution: Incoming.Execution) = recordTestAndExecution(execution, batch)
    val (testIds, executions) = incomingExecutions.map(recordExecution).unzip
    AffectedEntities(batch, executions, testIds)
  }

  private def recordTestAndExecution(incomingExecution: Incoming.Execution, batch: Batch): (Id[Test], EnrichedExecution) = {
    val testId = recordTest(incomingExecution.test)
    val execution = recordExecution(incomingExecution, batch, testId, batch.configurationOpt)
    (testId, execution)
  }

  private def recordTest(incomingTest: Incoming.Test): Id[Test] = {
    val test = Test(name = incomingTest.name, groupOpt = incomingTest.groupOpt)
    val testId = dao.ensureTestIsRecorded(test)
    recordCategories(testId, incomingTest.categories)
    testId
  }

  private def recordCategories(testId: Id[Test], categories: Seq[String]) {
    val existingCategories = dao.getCategories(testId)
    val (userCategories, importedCategories) = dao.getCategories(testId).partition(_.isUserCategory)
    dao.removeCategories(testId, importedCategories.map(_.category))
    val userCats = userCategories.map(_.category).toSet
    val newCategories = categories.distinct.filterNot(userCats.contains).map(c ⇒ TestCategory(testId, c, isUserCategory = false))
    dao.addCategories(newCategories)
  }

  private def recordExecution(incomingExecution: Incoming.Execution, batch: Batch, testId: Id[Test], batchConfigurationOpt: Option[Configuration]): EnrichedExecution = {
    val execution = createExecution(incomingExecution, batch, testId, batchConfigurationOpt)
    val executionId = dao.newExecution(execution, incomingExecution.logOpt)
    EnrichedExecution(
      execution = execution.copy(id = executionId),
      qualifiedName = incomingExecution.test.qualifiedName,
      batchNameOpt = batch.nameOpt,
      logOpt = incomingExecution.logOpt,
      commentOpt = None)
  }

  private def createExecution(incomingExecution: Incoming.Execution, batch: Batch, testId: Id[Test], batchConfigurationOpt: Option[Configuration]): Execution = {
    val executionTime = incomingExecution.executionTimeOpt getOrElse batch.executionTime
    val configuration = incomingExecution.configurationOpt orElse batchConfigurationOpt getOrElse Configuration.Default
    Execution(
      batchId = batch.id,
      testId = testId,
      executionTime = executionTime,
      durationOpt = incomingExecution.durationOpt,
      summaryOpt = incomingExecution.summaryOpt,
      passed = incomingExecution.passed,
      configuration = configuration)
  }

  private def recordBatchOnly(incomingBatch: Incoming.Batch): Batch = {
    val batch = createBatch(incomingBatch)
    val batchId = dao.newBatch(batch, incomingBatch.logOpt)
    batch.copy(id = batchId)
  }

  /**
   * If there is exactly one configuration amongst the executions, and the batch is complete, infer that the batch
   *  also has that configuration.
   */
  private def inferConfiguration(complete: Boolean, executions: Seq[Incoming.Execution]): Option[Configuration] =
    condOpt(executions.flatMap(_.configurationOpt).distinct) {
      case Seq(configuration) if complete ⇒ configuration
    }

  private def createBatch(incomingBatch: Incoming.Batch): Batch = {
    val executions = incomingBatch.executions
    val batchPassed = executions.forall(_.passed)
    val executionTime = incomingBatch.executionTimeOpt getOrElse clock.now
    val configurationOpt = incomingBatch.configurationOpt orElse inferConfiguration(incomingBatch.complete, executions)
    Batch(
      urlOpt = incomingBatch.urlOpt,
      executionTime = executionTime,
      durationOpt = incomingBatch.durationOpt,
      nameOpt = incomingBatch.nameOpt,
      passed = batchPassed,
      totalCount = executions.size,
      passCount = executions.count(_.passed),
      failCount = executions.count(_.failed),
      configurationOpt = configurationOpt)
  }

}