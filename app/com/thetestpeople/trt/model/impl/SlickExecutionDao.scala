package com.thetestpeople.trt.model.impl

import org.joda.time.Interval

import com.thetestpeople.trt.model._

trait SlickExecutionDao extends ExecutionDao { this: SlickDao ⇒

  import driver.simple._
  import Database.dynamicSession
  import jodaSupport._
  import Mappers._
  import Tables._

  def getEnrichedExecution(id: Id[Execution]): Option[EnrichedExecution] = {
    val query =
      for {
        ((execution, test, batch), log) ← executionTestBatchJoin leftJoin executionLogs on (_._1.id === _.executionId)
        if execution.id === id
      } yield (execution, test.name, test.group, batch.name, log.?)
    query.firstOption.map {
      case (execution, testName, testGroupOpt, batchNameOpt, logOpt) ⇒
        EnrichedExecution(execution, QualifiedName(testName, testGroupOpt), batchNameOpt, logOpt.map(_.log))
    }
  }

  private lazy val executionTestBatchJoin =
    for {
      execution ← executions
      test ← tests
      batch ← batches
      if test.id === execution.testId
      if batch.id === execution.batchId
    } yield (execution, test, batch)

  def getEnrichedExecutionsInBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean]): List[EnrichedExecution] = {
    var query =
      for {
        (execution, test, batch) ← executionTestBatchJoin
        if batch.id === batchId
      } yield (execution, test.name, test.group, batch.name)
    for (passedFilter ← passedFilterOpt)
      query = query.filter { case (execution, _, _, _) ⇒ execution.passed === passedFilter }
    query.run.toList.map {
      case (execution, testName, testGroupOpt, batchNameOpt) ⇒
        EnrichedExecution(execution, QualifiedName(testName, testGroupOpt), batchNameOpt, logOpt = None)
    }
  }

  def iterateAllExecutions[T](f: Iterator[ExecutionLite] ⇒ T): T =
    executions
      .sortBy(e ⇒ (e.configuration, e.testId, e.executionTime))
      .map(e ⇒ (e.configuration, e.testId, e.executionTime, e.passed))
      .iterator
      .map(ExecutionLite.tupled)
      .use(f)
  // We would like to setFetchSize hint to avoid loading all executions into memory, however this is an outstanding 
  // feature request on Slick: https://github.com/slick/slick/issues/41

  def getExecutionIntervalsByConfig(): Map[Configuration, Interval] =
    executions.groupBy(_.configuration).map {
      case (configuration, executions) ⇒
        val executionTimes = executions.map(_.executionTime)
        (configuration, executionTimes.min, executionTimes.max)
    }.run.collect {
      case (configuration, Some(earliest), Some(latest)) ⇒ configuration -> new Interval(earliest, latest)
    }.toMap

  def getEnrichedExecutions(configurationOpt: Option[Configuration], startingFrom: Int, limit: Int): List[EnrichedExecution] = {
    var query =
      for ((execution, test, batch) ← executionTestBatchJoin)
        yield (execution, test.name, test.group, batch.name)
    for (configuration ← configurationOpt)
      query = query.filter(_._1.configuration === configuration)
    query = query.sortBy(_._1.executionTime.desc)
    query = query.drop(startingFrom).take(limit)
    query.run.toList.map {
      case (execution, testName, testGroupOpt, batchNameOpt) ⇒
        EnrichedExecution(execution, QualifiedName(testName, testGroupOpt), batchNameOpt, logOpt = None)
    }
  }

  def countExecutions(configurationOpt: Option[Configuration]): Int =
    (configurationOpt match {
      case None                ⇒ executions
      case Some(configuration) ⇒ executions.filter(_.configuration === configuration)
    }).map(_.id).countDistinct.run

  def getExecutionLog(id: Id[Execution]): Option[String] =
    executionLogs.filter(_.executionId === id).map(_.log).firstOption

  private lazy val getExecutionsForTestCompiled = {
    def getExecutionsForTest(id: Column[Id[Test]]) =
      executions.filter(_.testId === id).sortBy(_.executionTime.desc)
    Compiled(getExecutionsForTest _)
  }

  def getExecutionsForTest(id: Id[Test]): List[Execution] =
    getExecutionsForTestCompiled(id).run.toList

  def getEnrichedExecutionsForTest(id: Id[Test], configurationOpt: Option[Configuration]): List[EnrichedExecution] = {
    var query =
      for {
        (execution, test, batch) ← executionTestBatchJoin
        if test.id === id
      } yield (execution, test.name, test.group, batch.name)
    for (configuration ← configurationOpt)
      query = query.filter(_._1.configuration === configuration)
    query = query.sortBy(_._1.executionTime.desc)
    query.run.toList.map {
      case (execution, testName, testGroupOpt, batchNameOpt) ⇒
        EnrichedExecution(execution, QualifiedName(testName, testGroupOpt), batchNameOpt, logOpt = None)
    }
  }
}