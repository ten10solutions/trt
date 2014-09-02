package com.thetestpeople.trt.service

import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils._

class BatchRecorder(dao: Dao, clock: Clock, analysisService: AnalysisService) extends HasLogger {

  def recordBatch(incomingBatch: Incoming.Batch): Batch = Utils.time("recordBatch") {
    val (testIds, batch) = dao.transaction {
      val batch = createNewBatch(incomingBatch)
      val (testIds, executionIds) = incomingBatch.executions.map(recordTestAndExecution(_, batch)).unzip
      if (false)
        luceneStuff(executionIds)
      (testIds, batch)
    }
    analysisService.scheduleAnalysis(testIds)
    batch
  }

  // TODO
  private def luceneStuff(executionIds: List[Id[Execution]]) {
    val executions = for {
      executionId ← executionIds
      execution ← dao.getEnrichedExecution(executionId)
    } yield execution
    val luceneIndexer = new LuceneIndexer
    luceneIndexer.addExecutions(executions)
  }

  private def recordTestAndExecution(incomingExecution: Incoming.Execution, batch: Batch): (Id[Test], Id[Execution]) = {
    val test = Test(name = incomingExecution.test.name, groupOpt = incomingExecution.test.groupOpt)
    val testId = dao.ensureTestIsRecorded(test)
    val execution = createNewExecution(incomingExecution, batch, testId)
    (testId, execution.id)
  }

  private def createNewExecution(incomingExecution: Incoming.Execution, batch: Batch, testId: Id[Test]): Execution = {
    val executionTime = incomingExecution.executionTimeOpt.getOrElse(batch.executionTime)
    val configuration = incomingExecution.configurationOpt.getOrElse(Configuration.Default)
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

  private def makeBatch(incomingBatch: Incoming.Batch): Batch = {
    val executions = incomingBatch.executions
    val batchPassed = executions.forall(_.passed)
    val executionTime = incomingBatch.executionTimeOpt.getOrElse(clock.now)
    Batch(
      urlOpt = incomingBatch.urlOpt,
      executionTime = executionTime,
      durationOpt = incomingBatch.durationOpt,
      nameOpt = incomingBatch.nameOpt,
      passed = batchPassed,
      totalCount = executions.size,
      passCount = executions.count(_.passed),
      failCount = executions.count(_.failed))
  }

}