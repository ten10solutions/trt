package com.thetestpeople.trt.model.impl

import org.joda.time.Interval
import com.thetestpeople.trt.model._
import scala.slick.ast.Node
import scala.slick.driver.JdbcProfile
import scala.slick.lifted.RunnableCompiled

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

  private def makeEnrichedExecution(execution: Execution, testName: String, testGroupOpt: Option[String], batchNameOpt: Option[String]): EnrichedExecution =
    EnrichedExecution(execution, QualifiedName(testName, testGroupOpt), batchNameOpt, logOpt = None)

  def getEnrichedExecutions(ids: Seq[Id[Execution]]): Seq[EnrichedExecution] = {
    var query =
      for {
        (execution, test, batch) ← executionTestBatchJoin
        if execution.id inSet ids
      } yield (execution, test.name, test.group, batch.name)
    query.run.map((makeEnrichedExecution _).tupled)
  }

  def getEnrichedExecutionsInBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean]): Seq[EnrichedExecution] = {
    var query =
      for {
        (execution, test, batch) ← executionTestBatchJoin
        if batch.id === batchId
      } yield (execution, test.name, test.group, batch.name)
    for (passedFilter ← passedFilterOpt)
      query = query.filter { case (execution, _, _, _) ⇒ execution.passed === passedFilter }
    query.run.map((makeEnrichedExecution _).tupled)
  }

  // Workaround for setting setFetchSize (from https://github.com/slick/slick/issues/809)
  import scala.language.higherKinds
  private def createBatchInvoker[U](n: Node, param: Any, fetchSize: Int)(implicit driver: JdbcProfile): driver.QueryInvoker[U] =
    new driver.QueryInvoker[U](n, param) {
      override def setParam(st: java.sql.PreparedStatement) {
        super.setParam(st)
        st.setFetchSize(fetchSize)
      }
    }
  private def batch[U, C[_]](query: Query[_, U, C], fetchSize: Int)(implicit driver: JdbcProfile) =
    createBatchInvoker[U](driver.queryCompiler.run(query.toNode).tree, (), fetchSize)

  private def executionsOfNonDeletedTests =
    for {
      execution ← executions
      test ← tests
      if execution.testId === test.id
      if test.deleted === false
    } yield execution

  def iterateAllExecutions[T](f: Iterator[ExecutionLite] ⇒ T): T = {
    val ExecutionBatchSize = 1000
    val query = batch(
      fetchSize = ExecutionBatchSize,
      query = executionsOfNonDeletedTests
        .sortBy(e ⇒ (e.configuration, e.testId, e.executionTime))
        .map(e ⇒ (e.configuration, e.testId, e.executionTime, e.passed)))
    query.iterator
      .map(ExecutionLite.tupled)
      .use(f)
  }

  def getExecutionIntervalsByConfig(): Map[Configuration, Interval] =
    executions.groupBy(_.configuration).map {
      case (configuration, executions) ⇒
        val executionTimes = executions.map(_.executionTime)
        (configuration, executionTimes.min, executionTimes.max)
    }.run.collect {
      case (configuration, Some(earliest), Some(latest)) ⇒ configuration -> new Interval(earliest, latest)
    }.toMap

  def getEnrichedExecutions(configurationOpt: Option[Configuration], startingFrom: Int, limit: Int): Seq[EnrichedExecution] = {
    var query =
      for ((execution, test, batch) ← executionTestBatchJoin)
        yield (execution, test.name, test.group, batch.name)
    for (configuration ← configurationOpt)
      query = query.filter(_._1.configuration === configuration)
    query = query.sortBy(_._1.executionTime.desc)
    query = query.drop(startingFrom).take(limit)
    query.run.map((makeEnrichedExecution _).tupled)
  }

  def countExecutions(configurationOpt: Option[Configuration]): Int = {
    (configurationOpt match {
      case None                ⇒ executionCountCache.get
      case Some(configuration) ⇒ executions.filter(_.configuration === configuration).length.run
    })
    //    StaticQuery.queryNA[Int]("""select count(*) from "executions"""").first
  }

  private def countAllExecutions(): Int = executions.length.run
  
  protected val executionCountCache: Cache[Int] = Cache { countAllExecutions() }
  
  def getExecutionLog(id: Id[Execution]): Option[String] =
    executionLogs.filter(_.executionId === id).map(_.log).firstOption

  private lazy val getExecutionsForTestCompiled = {
    def getExecutionsForTest(id: Column[Id[Test]]) =
      executions.filter(_.testId === id).sortBy(_.executionTime.desc)
    Compiled(getExecutionsForTest _)
  }

  def getExecutionsForTest(id: Id[Test]): Seq[Execution] =
    getExecutionsForTestCompiled(id).run.toList

  def getEnrichedExecutionsForTest(id: Id[Test], configurationOpt: Option[Configuration]): Seq[EnrichedExecution] = {
    var query =
      for {
        (execution, test, batch) ← executionTestBatchJoin
        if test.id === id
      } yield (execution, test.name, test.group, batch.name)
    for (configuration ← configurationOpt)
      query = query.filter(_._1.configuration === configuration)
    query = query.sortBy(_._1.executionTime.desc)
    query.run.toList.map((makeEnrichedExecution _).tupled)
  }
}