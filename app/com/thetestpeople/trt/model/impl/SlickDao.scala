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
import scala.slick.driver.H2Driver

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
    val executionComments = TableQuery[ExecutionCommentMapping]
    val batchComments = TableQuery[BatchCommentMapping]
    val testComments = TableQuery[TestCommentMapping]
    val systemConfiguration = TableQuery[SystemConfigurationMapping]

    val jenkinsJobs = TableQuery[JenkinsJobMapping]
    val ciBuilds = TableQuery[CiBuildMapping]
    val ciImportSpecs = TableQuery[CiImportSpecMapping]
    val jenkinsConfiguration = TableQuery[JenkinsConfigurationMapping]
    val jenkinsJobParams = TableQuery[JenkinsJobParamMapping]

  }

  object Mappers {

    implicit def idMapper[T <: EntityType] = MappedColumnType.base[Id[T], Int](_.value, Id[T])

    implicit def durationMapper = MappedColumnType.base[Duration, Long](_.getMillis, Duration.millis)

    implicit def statusMapper = MappedColumnType.base[TestStatus, String](TestStatus.oldLabel, TestStatus.parseOld)

    implicit def configurationMapper = MappedColumnType.base[Configuration, String](_.configuration, Configuration.apply)

    implicit def ciTypeMapper = MappedColumnType.base[CiType, String](_.name, CiType.apply)

    implicit def uriMapper = MappedColumnType.base[URI, String](_.toString, new URI(_))

  }

  import Tables._
  import Mappers._

  def transaction[T](p: ⇒ T): T = database.withDynTransaction { p }

  def deleteAll() = Cache.invalidate(configurationsCache, executionCountCache) {

    transaction {
      ciBuilds.delete
      ciImportSpecs.delete
      jenkinsJobs.delete
      jenkinsJobParams.delete
      batchLogs.delete
      executionComments.delete
      batchComments.delete
      testComments.delete
      analyses.delete
    }

    if (driver.isInstanceOf[H2Driver]) {
      // H2 really struggles with bulk deleting large numbers of executions, so we do it in batches:
      var continue = true
      while (continue) {
        transaction {
          val ids = executions.map(_.id).take(5000).run
          if (ids.isEmpty)
            continue = false
          else {
            executionLogs.filter(_.executionId inSet ids).delete
            executions.filter(_.id inSet ids).delete
          }
        }
        logger.debug("Deleted 5000")
      }
    } else {
      transaction {
        executionLogs.delete
        executions.delete
      }
    }

    transaction {
      batches.delete
      tests.delete
      jenkinsConfiguration.delete
      systemConfiguration.delete
      systemConfiguration.insert(SystemConfiguration())

      val defaultJenkinsConfiguration = JenkinsConfiguration()
      jenkinsConfiguration.insert(JenkinsConfiguration())
    }

    logger.info("Deleted all data")
  }

  private val testsAndAnalyses =
    for ((test, analysis) ← tests leftJoin analyses on (_.id === _.testId))
      yield (test, analysis)

  def getTestAndAnalysis(id: Id[Test], configuration: Configuration): Option[TestAndAnalysis] = {
    val query =
      for {
        ((test, analysis), comment) ← testsAndAnalyses leftJoin testComments on (_._1.id === _.testId)
        if test.id === id
        if analysis.configuration === configuration
      } yield (test, analysis.?, comment.?)
    query.firstOption.map { case (test, analysisOpt, commentOpt) ⇒ TestAndAnalysis(test, analysisOpt, commentOpt.map(_.text)) }
  }

  def getTestIds(): Seq[Id[Test]] =
    tests.map(_.id).run

  def getTestNames(pattern: String): Seq[String] =
    tests.filter(_.name.toLowerCase like globToSqlPattern(pattern)).map(_.name).run

  def getGroups(pattern: String): Seq[String] =
    tests
      .filter(_.group.isDefined)
      .filter(_.group.toLowerCase like globToSqlPattern(pattern))
      .groupBy(_.group).map(_._1).run.flatten

  private def globToSqlPattern(pattern: String) = pattern.replace("*", "%").toLowerCase

  private type TestAnalysisQuery = Query[(TestMapping, AnalysisMapping), (Test, Analysis), Seq]

  def getAnalysedTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None,
    sortBy: SortBy.Test = SortBy.Test.Group()): Seq[TestAndAnalysis] = {
    var query: TestAnalysisQuery = testsAndAnalyses
    query = query.filter(_._2.configuration === configuration)
    query = query.filterNot(_._1.deleted)
    for (name ← nameOpt)
      query = query.filter(_._1.name.toLowerCase like globToSqlPattern(name))
    for (group ← groupOpt)
      query = query.filter(_._1.group.toLowerCase like globToSqlPattern(group))
    for (status ← testStatusOpt)
      query = query.filter(_._2.status === status)
    query = sortQuery(query, sortBy)
    query = query.drop(startingFrom)
    for (limit ← limitOpt)
      query = query.take(limit)
    query.map { case (test, analysis) ⇒ (test, analysis.?) }.run.map { case (test, analysisOpt) ⇒ TestAndAnalysis(test, analysisOpt, commentOpt = None) }
  }

  private def sortQuery(query: TestAnalysisQuery, sortBy: SortBy.Test): TestAnalysisQuery =
    sortBy match {
      case SortBy.Test.Weather(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.weather, descending) }
      case SortBy.Test.Group(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(test.name, descending) }
          .sortBy { case (test, analysis) ⇒ order(test.group, descending) }
      case SortBy.Test.Name(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(test.name, descending) }
      case SortBy.Test.Duration(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.medianDuration, descending) }
      case SortBy.Test.ConsecutiveFailures(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.consecutiveFailures, descending) }
      case SortBy.Test.StartedFailing(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.failingSince, descending) }
      case SortBy.Test.LastPassed(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.lastPassedTime, descending) }
      case SortBy.Test.LastFailed(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.lastFailedTime, descending) }
    }

  private def order[T](column: Column[T], descending: Boolean) =
    if (descending) column.desc else column.asc

  def getTestsById(testIds: Seq[Id[Test]]): Seq[Test] = tests.filter(_.id inSet testIds).run

  def getDeletedTests(): Seq[Test] = tests.filter(_.deleted).sortBy(_.name).sortBy(_.group).run

  def getTestCounts(configuration: Configuration, nameOpt: Option[String] = None, groupOpt: Option[String] = None): TestCounts = {
    var query = testsAndAnalyses
    query = query.filter(_._2.configuration === configuration)
    query = query.filterNot(_._1.deleted)
    for (name ← nameOpt)
      query = query.filter(_._1.name.toLowerCase like globToSqlPattern(name))
    for (group ← groupOpt)
      query = query.filter(_._1.group.toLowerCase like globToSqlPattern(group))
    // Workaround for Slick exception if no analysis: "scala.slick.SlickException: Read NULL value for ResultSet column":
    query = query.filter(_._2.testId.?.isDefined)
    val results: Map[TestStatus, Int] =
      query.groupBy(_._2.status).map { case (status, results) ⇒ status -> results.length }.run.toMap
    def count(status: TestStatus) = results.collect { case (`status`, count) ⇒ count }.headOption.getOrElse(0)
    TestCounts(passed = count(TestStatus.Healthy), warning = count(TestStatus.Warning), failed = count(TestStatus.Broken))
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
    val countRecords: Seq[CountRecord] =
      baseQuery.groupBy {
        case (test, analysis) ⇒ (analysis.configuration, analysis.status)
      }.map {
        case ((configuration, status), results) ⇒ (configuration, status, results.length)
      }.run.map(CountRecord.tupled)
    def testCounts(countRecords: Seq[CountRecord]): TestCounts = {
      def count(status: TestStatus) = countRecords.find(_.status == status).map(_.count).getOrElse(0)
      TestCounts(
        passed = count(TestStatus.Healthy),
        warning = count(TestStatus.Warning),
        failed = count(TestStatus.Broken))
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
      getTestWithName(name).filter(_._1.group.isEmpty)
    Compiled(getTestWithNameAndNoGroup _)
  }

  private def getTestAndAnalysis(qualifiedName: QualifiedName): Option[TestAndAnalysis] = {
    val QualifiedName(name, groupOpt) = qualifiedName
    val query = groupOpt match {
      case Some(group) ⇒ getTestWithGroupCompiled(name, group)
      case None        ⇒ getTestWithoutGroupCompiled(name)
    }
    query.firstOption.map { case (test, analysisOpt) ⇒ TestAndAnalysis(test, analysisOpt, commentOpt = None) }
  }

  def getBatch(id: Id[Batch]): Option[BatchAndLog] = {
    val query =
      for {
        (((batch, log), jenkinsBuild), comment) ← batches leftJoin batchLogs on (_.id === _.batchId) leftJoin ciBuilds on (_._1.id === _.batchId) leftJoin batchComments on (_._1._1.id === _.batchId)
        if batch.id === id
      } yield (batch, log.?, jenkinsBuild.?, comment.?)
    query.firstOption.map { case (batch, logRowOpt, jenkinsBuildOpt, commentOpt) ⇒ BatchAndLog(batch, logRowOpt.map(_.log), jenkinsBuildOpt.flatMap(_.importSpecIdOpt), commentOpt = commentOpt.map(_.text)) }
  }

  def getBatches(jobOpt: Option[Id[JenkinsJob]] = None, configurationOpt: Option[Configuration] = None): Seq[Batch] = {
    var baseQuery =
      jobOpt match {
        case Some(jobId) ⇒
          for {
            batch ← batches
            jenkinsBuild ← ciBuilds
            if jenkinsBuild.batchId === batch.id
            if jenkinsBuild.jobId === jobId
          } yield batch
        case None ⇒
          batches
      }
    for (configuration ← configurationOpt)
      baseQuery = baseQuery.filter(_.configuration === configuration)
    baseQuery.sortBy(_.executionTime.desc).run
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

  private def deleteTestsWithoutExecutions(testIds: Seq[Id[Test]]): Seq[Id[Test]] = {
    val testsWithoutExecutionsQuery =
      for {
        (test, execution) ← tests leftJoin executions on (_.id === _.testId)
        if test.id inSet testIds
        if execution.id.?.isEmpty
      } yield test.id

    val testIdsToDelete = testsWithoutExecutionsQuery.run
    testComments.filter(_.testId inSet testIdsToDelete).delete
    tests.filter(_.id inSet testIdsToDelete).delete
    testIdsToDelete
  }

  def deleteBatches(batchIds: Seq[Id[Batch]]): DeleteBatchResult =
    Cache.invalidate(configurationsCache, executionCountCache) {
      val (executionIds, testIds) = executions.filter(_.batchId inSet batchIds).map(e ⇒ (e.id, e.testId)).run.unzip
      ciBuilds.filter(_.batchId inSet batchIds).delete
      analyses.filter(_.testId inSet testIds).delete
      executionLogs.filter(_.executionId inSet executionIds).delete
      executionComments.filter(_.executionId inSet executionIds).delete
      executions.filter(_.id inSet executionIds).delete
      batchLogs.filter(_.batchId inSet batchIds).delete
      batchComments.filter(_.batchId inSet batchIds).delete
      batches.filter(_.id inSet batchIds).delete
      val deletedTestIds = deleteTestsWithoutExecutions(testIds).toSet
      val remainingTestIds = testIds.filterNot(deletedTestIds.contains)
      DeleteBatchResult(remainingTestIds, executionIds)
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

  def newExecution(execution: Execution, logOpt: Option[String]): Id[Execution] = Cache.invalidate(configurationsCache, executionCountCache) {
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

  private val configurationsCache: Cache[Seq[Configuration]] = Cache { rawGetConfigurations() }

  def getConfigurations(): Seq[Configuration] = configurationsCache.get

  private def rawGetConfigurations(): Seq[Configuration] =
    executions.groupBy(_.configuration).map(_._1).sorted.run

  def getConfigurations(testId: Id[Test]): Seq[Configuration] =
    executions.filter(_.testId === testId).groupBy(_.configuration).map(_._1).sorted.run

  def setBatchComment(id: Id[Batch], text: String) =
    if (batchComments.filter(_.batchId === id).firstOption.isDefined)
      batchComments.filter(_.batchId === id).map(_.text).update(text)
    else
      batchComments.insert(BatchComment(id, text))

  def deleteBatchComment(id: Id[Batch]) = batchComments.filter(_.batchId === id).delete

  def setTestComment(id: Id[Test], text: String) =
    if (testComments.filter(_.testId === id).firstOption.isDefined)
      testComments.filter(_.testId === id).map(_.text).update(text)
    else
      testComments.insert(TestComment(id, text))

  def deleteTestComment(id: Id[Test]) = testComments.filter(_.testId === id).delete

}

