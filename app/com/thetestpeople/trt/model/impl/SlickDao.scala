package com.thetestpeople.trt.model.impl

import org.joda.time._
import com.github.tototoshi.slick.GenericJodaSupport
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.KeyedLocks
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.utils.LockUtils._
import javax.sql.DataSource
import java.net.URI
import scala.slick.util.CloseableIterator

class SlickDao(jdbcUrl: String, dataSourceOpt: Option[DataSource] = None) extends Dao
    with DaoAdmin with HasLogger with SlickJenkinsDao with SlickExecutionDao with Mappings {

  protected val driver = DriverLookup(jdbcUrl)

  import driver.simple._

  import Database.dynamicSession

  private val database = dataSourceOpt match {
    case Some(dataSource) ⇒ Database.forDataSource(dataSource)
    case None             ⇒ Database.forURL(jdbcUrl)
  }

  protected val jodaSupport = new GenericJodaSupport(driver)

  import jodaSupport._

  object Tables {

    val batches = TableQuery[BatchMapping]
    val executions = TableQuery[ExecutionMapping]
    val tests = TableQuery[TestMapping]
    val analyses = TableQuery[AnalysisMapping]
    val executionLogs = TableQuery[ExecutionLogMapping]
    val batchLogs = TableQuery[BatchLogMapping]
    val systemConfiguration = TableQuery[SystemConfigurationMapping]

    val jenkinsJobs = TableQuery[JenkinsJobMapping]
    val jenkinsBuilds = TableQuery[JenkinsBuildMapping]
    val jenkinsImportSpecs = TableQuery[JenkinsImportSpecMapping]
    val jenkinsConfiguration = TableQuery[JenkinsConfigurationMapping]
    val jenkinsJobParams = TableQuery[JenkinsJobParamMapping]

  }

  object Mappers {

    implicit def idMapper[T <: EntityType] = MappedColumnType.base[Id[T], Int](_.value, Id[T])

    implicit def durationMapper = MappedColumnType.base[Duration, Long](_.getMillis, Duration.millis)

    implicit def statusMapper = MappedColumnType.base[TestStatus, String](_.toString, TestStatus.parse)

    implicit def configurationMapper = MappedColumnType.base[Configuration, String](_.configuration, Configuration.apply)

    implicit def uriMapper = MappedColumnType.base[URI, String](_.toString, new URI(_))

  }

  import Tables._
  import Mappers._

  def transaction[T](p: ⇒ T): T = {
    database.withDynTransaction {
      p
    }
  }

  def deleteAll() = transaction {
    jenkinsImportSpecs.delete
    jenkinsBuilds.delete
    jenkinsJobs.delete
    jenkinsJobParams.delete
    jenkinsConfiguration.delete
    systemConfiguration.delete
    batchLogs.delete
    executionLogs.delete
    analyses.delete
    executions.delete
    batches.delete
    tests.delete
    val DefaultSystemConfiguration = SystemConfiguration()
    systemConfiguration.insert(DefaultSystemConfiguration)

    val DefaultJenkinsConfiguration = JenkinsConfiguration()
    jenkinsConfiguration.insert(DefaultJenkinsConfiguration)
    logger.info("Deleted all data")
  }

  private val testsAndAnalyses =
    for ((test, analysis) ← tests leftJoin analyses on (_.id === _.testId))
      yield (test, analysis)

  def getTestAndAnalysis(id: Id[Test], configuration: Configuration): Option[TestAndAnalysis] = {
    val query =
      for {
        (test, analysis) ← testsAndAnalyses
        if test.id === id
        if analysis.configuration === configuration
      } yield (test, analysis.?)
    query.firstOption.map(TestAndAnalysis.tupled)
  }

  def getTestIds(): List[Id[Test]] =
    tests.map(_.id).run.toList

  def getAnalysedTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None): List[TestAndAnalysis] = {

    var query = testsAndAnalyses.filter(_._2.configuration === configuration).filterNot(_._1.deleted)
    for (group ← groupOpt)
      query = query.filter(_._1.group === group)
    for (status ← testStatusOpt)
      query = query.filter(_._2.status === status)
    query = query.sortBy(_._1.name).sortBy(_._1.group)
    query = query.drop(startingFrom)
    for (limit ← limitOpt)
      query = query.take(limit)
    query.map { case (test, analysis) ⇒ (test, analysis.?) }.run.map(TestAndAnalysis.tupled).toList
  }

  def getTestsById(testIds: List[Id[Test]]): List[Test] = tests.filter(_.id inSet testIds).run.toList

  def getTestCounts(configuration: Configuration, groupOpt: Option[String] = None): TestCounts = {
    var query = testsAndAnalyses.filter(_._2.configuration === configuration).filterNot(_._1.deleted)
    for (group ← groupOpt)
      query = query.filter(_._1.group === group)
    // Workaround for Slick exception if no analysis: "scala.slick.SlickException: Read NULL value for ResultSet column":
    query = query.filter(_._2.testId.isNotNull)
    val results: Map[TestStatus, Int] =
      query.groupBy(_._2.status).map { case (status, results) ⇒ status -> results.length }.run.toMap
    def count(status: TestStatus) = results.collect { case (`status`, count) ⇒ count }.headOption.getOrElse(0)
    TestCounts(passed = count(TestStatus.Pass), warning = count(TestStatus.Warn), failed = count(TestStatus.Fail))
  }

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts] = {
    val baseQuery =
      for {
        test ← tests
        analysis ← analyses
        if analysis.testId === test.id
        if !test.deleted
      } yield (test, analysis)
    case class CountRecord(configuration: Configuration, status: TestStatus, count: Int)
    val countRecords: List[CountRecord] =
      baseQuery.groupBy {
        case (test, analysis) ⇒ (analysis.configuration, analysis.status)
      }.map {
        case ((configuration, status), results) ⇒ (configuration, status, results.length)
      }.run.toList.map(CountRecord.tupled)
    def testCounts(countRecords: List[CountRecord]): TestCounts = {
      def count(status: TestStatus) = countRecords.find(_.status == status).map(_.count).getOrElse(0)
      TestCounts(
        passed = count(TestStatus.Pass),
        warning = count(TestStatus.Warn),
        failed = count(TestStatus.Fail))
    }
    countRecords.groupBy(_.configuration).map {
      case (configuration, countRecords) ⇒ configuration -> testCounts(countRecords)
    }
  }

  private def getTestWithName(name: Column[String]) =
    for {
      (test, analysis) ← testsAndAnalyses
      if test.name === name
    } yield (test, analysis.?)

  private val getTestWithGroupCompiled = {
    def getTestWithNameAndGroup(name: Column[String], group: Column[String]) =
      getTestWithName(name).filter(_._1.group === group)
    Compiled(getTestWithNameAndGroup _)
  }

  private val getTestWithoutGroupCompiled = {
    def getTestWithNameAndNoGroup(name: Column[String]) =
      getTestWithName(name).filter(_._1.group.isNull)
    Compiled(getTestWithNameAndNoGroup _)
  }

  private def getTestAndAnalysis(qualifiedName: QualifiedName): Option[TestAndAnalysis] = {
    val QualifiedName(name, groupOpt) = qualifiedName
    val query = groupOpt match {
      case Some(group) ⇒ getTestWithGroupCompiled(name, group)
      case None        ⇒ getTestWithoutGroupCompiled(name)
    }
    query.firstOption.map(TestAndAnalysis.tupled)
  }

  def getBatch(id: Id[Batch]): Option[BatchAndLog] = {
    val query =
      for {
        (batch, log) ← batches leftJoin batchLogs on (_.id === _.batchId)
        if batch.id === id
      } yield (batch, log.?)
    query.firstOption.map { case (batch, logRowOpt) ⇒ BatchAndLog(batch, logRowOpt.map(_.log)) }
  }

  def getBatches(jobOpt: Option[Id[JenkinsJob]] = None, configurationOpt: Option[Configuration] = None): List[Batch] = {
    var baseQuery =
      jobOpt match {
        case Some(jobId) ⇒
          for {
            batch ← batches
            jenkinsBuild ← jenkinsBuilds
            if jenkinsBuild.batchId === batch.id
            if jenkinsBuild.jobId === jobId
          } yield batch
        case None ⇒
          batches
      }
    for (configuration ← configurationOpt)
      baseQuery = baseQuery.filter(_.configuration === configuration)
    baseQuery.sortBy(_.executionTime.desc).run.toList
  }

  def newBatch(batch: Batch, logOpt: Option[String]): Id[Batch] = {
    val batchId = (batches returning batches.map(_.id)).insert(batch)
    for (log ← logOpt)
      batchLogs.insert(BatchLogRow(batchId, removeNullChars(log)))
    batchId
  }

  /**
   *  Postgres throws an error on null chars:
   *    "ERROR: invalid byte sequence for encoding "UTF8": 0x00"
   */
  private def removeNullChars(s: String) = s.filterNot(_ == '\u0000')

  private def deleteTestsWithoutExecutions(testIds: List[Id[Test]]): List[Id[Test]] = {
    val testsWithoutExecutionsQuery =
      for {
        (test, execution) ← tests leftJoin executions on (_.id === _.testId)
        if test.id inSet testIds
        if execution.id.isNull
      } yield test.id

    val testIdsToDelete = testsWithoutExecutionsQuery.run.toList
    tests.filter(_.id inSet testIdsToDelete).delete
    testIdsToDelete
  }

  def deleteBatches(batchIds: List[Id[Batch]]): List[Id[Test]] = {
    val (executionIds, testIds) = executions.filter(_.batchId inSet batchIds).map(e ⇒ (e.id, e.testId)).run.toList.unzip
    jenkinsBuilds.filter(_.batchId inSet batchIds).delete
    analyses.filter(_.testId inSet testIds).delete
    executionLogs.filter(_.executionId inSet executionIds).delete
    executions.filter(_.id inSet executionIds).delete
    batchLogs.filter(_.batchId inSet batchIds).delete
    batches.filter(_.id inSet batchIds).delete
    val deletedTestIds = deleteTestsWithoutExecutions(testIds).toSet
    testIds.filterNot(deletedTestIds.contains)
  }

  private val testInserter = (tests returning tests.map(_.id)).insertInvoker

  def ensureTestIsRecorded(test: Test): Id[Test] = synchronized {
    getTestAndAnalysis(test.qualifiedName) match {
      case Some(testAndAnalysis) ⇒
        testAndAnalysis.id
      case None ⇒
        testInserter.insert(test)
    }
  }

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true) =
    tests.filter(_.id.inSet(ids)).map(_.deleted).update(deleted)

  private val executionInserter = (executions returning executions.map(_.id)).insertInvoker
  private val executionLogInserter = executionLogs.insertInvoker

  def newExecution(execution: Execution, logOpt: Option[String]): Id[Execution] = {
    val executionId = executionInserter.insert(execution)
    for (log ← logOpt)
      executionLogInserter.insert(ExecutionLogRow(executionId, removeNullChars(log)))
    executionId
  }

  private val analysisInserter = analyses.insertInvoker

  private val updateAnalysisCompiled = {
    def updateAnalysis(testId: Column[Id[Test]], configuration: Column[Configuration]) =
      for {
        analysis ← analyses
        if analysis.testId === testId
        if analysis.configuration === configuration
      } yield analysis
    Compiled(updateAnalysis _)
  }

  private val getAnalysisCompiled = {
    def getAnalysis(testId: Column[Id[Test]], configuration: Column[Configuration]) =
      analyses.filter(_.testId === testId).filter(_.configuration === configuration)
    Compiled(getAnalysis _)
  }

  def upsertAnalysis(newAnalysis: Analysis) = synchronized {
    getAnalysisCompiled(newAnalysis.testId, newAnalysis.configuration).firstOption match {
      case Some(analysis) ⇒
        updateAnalysisCompiled(newAnalysis.testId, newAnalysis.configuration).update(newAnalysis)
      case None ⇒
        analysisInserter.insert(newAnalysis)
    }
  }

  def getSystemConfiguration(): SystemConfiguration =
    systemConfiguration.firstOption.getOrElse(throw new IllegalStateException("No system configuration present"))

  def updateSystemConfiguration(newConfig: SystemConfiguration) =
    systemConfiguration.update(newConfig)

  def getConfigurations(): List[Configuration] =
    executions.groupBy(_.configuration).map(_._1).sorted.run.toList

  def getConfigurations(testId: Id[Test]): Seq[Configuration] = 
    executions.filter(_.testId === testId).groupBy(_.configuration).map(_._1).sorted.run

}

