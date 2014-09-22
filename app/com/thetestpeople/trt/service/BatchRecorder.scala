package com.thetestpeople.trt.service

import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils._
import scala.PartialFunction.condOpt
import com.thetestpeople.trt.service.indexing.LogIndexer

class BatchRecorder(dao: Dao, clock: Clock, analysisService: AnalysisService, logIndexer: LogIndexer) extends HasLogger {

  def recordBatch(incomingBatch: Incoming.Batch): Batch = Utils.time("recordBatch") {
    val (executionIds, testIds, batch) = dao.transaction {
      val batch = createNewBatch(incomingBatch)
      def recordExecution(execution: Incoming.Execution) = recordTestAndExecution(execution, batch, incomingBatch.configurationOpt)
      val (testIds, executionIds) = incomingBatch.executions.map(recordExecution).unzip
      (executionIds, testIds, batch)
    }
    analysisService.scheduleAnalysis(testIds)
    indexExecutions(executionIds)
    batch
  }

  private def indexExecutions(executionIds: List[Id[Execution]]) {
    val executions = dao.transaction {
      for {
        executionId ← executionIds
        execution ← dao.getEnrichedExecution(executionId)
      } yield execution
    }
    logIndexer.addExecutions(executions)
  }

  private def recordTestAndExecution(incomingExecution: Incoming.Execution, batch: Batch, batchConfigurationOpt: Option[Configuration]): (Id[Test], Id[Execution]) = {
    val test = Test(name = incomingExecution.test.name, groupOpt = incomingExecution.test.groupOpt)
    val testId = dao.ensureTestIsRecorded(test)
    val execution = createNewExecution(incomingExecution, batch, testId, batchConfigurationOpt)
    (testId, execution.id)
  }

  private def createNewExecution(incomingExecution: Incoming.Execution, batch: Batch, testId: Id[Test], batchConfigurationOpt: Option[Configuration]): Execution = {
    val executionTime = incomingExecution.executionTimeOpt getOrElse batch.executionTime
    val configuration = incomingExecution.configurationOpt orElse batchConfigurationOpt getOrElse Configuration.Default
    val execution = Execution(
      batchId = batch.id,
      testId = testId,
      executionTime = executionTime,
      durationOpt = incomingExecution.durationOpt,
      summaryOpt = incomingExecution.summaryOpt,
      passed = incomingExecution.passed,
      configuration = configuration)
    val executionId = dao.newExecution(execution, incomingExecution.logOpt)
    execution.copy(id = executionId)
  }

  private def createNewBatch(incomingBatch: Incoming.Batch): Batch = {
    val batch = makeBatch(incomingBatch)
    val batchId = dao.newBatch(batch, incomingBatch.logOpt)
    logger.info(s"New batch recorded, name = ${incomingBatch.nameOpt}, id = ${batchId}, executions = ${incomingBatch.executions.size}")
    batch.copy(id = batchId)
  }

  /**
   * If there is exactly one configuration amongst the executions, infer that the batch has that configuration also
   */
  private def inferConfiguration(executions: Seq[Incoming.Execution]): Option[Configuration] =
    condOpt(executions.flatMap(_.configurationOpt).distinct) {
      case Seq(configuration) ⇒ configuration
    }

  private def makeBatch(incomingBatch: Incoming.Batch): Batch = {
    val executions = incomingBatch.executions
    val batchPassed = executions.forall(_.passed)
    val executionTime = incomingBatch.executionTimeOpt getOrElse clock.now
    val configurationOpt = incomingBatch.configurationOpt orElse inferConfiguration(executions)
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