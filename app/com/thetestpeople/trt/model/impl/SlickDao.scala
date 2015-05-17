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

class SlickDao(jdbcUrl: String, dataSourceOpt: Option[DataSource] = None)
    extends Dao
    with HasLogger
    with Mappings
    with SlickCiDao
    with SlickExecutionDao
    with SlickTestDao
    with SlickBatchDao {

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
    val testCategories = TableQuery[TestCategoryMapping]
    val ignoredTestConfigurations = TableQuery[IgnoredTestConfigurationMapping]
    val systemConfiguration = TableQuery[SystemConfigurationMapping]

    val ciJobs = TableQuery[CiJobMapping]
    val ciBuilds = TableQuery[CiBuildMapping]
    val ciImportSpecs = TableQuery[CiImportSpecMapping]
    val teamCityConfiguration = TableQuery[TeamCityConfigurationMapping]
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
      ciJobs.delete
      jenkinsJobParams.delete
      batchLogs.delete
      executionComments.delete
      batchComments.delete
      testComments.delete
      testCategories.delete
      analyses.delete
    }

    deleteAllExecutions()

    transaction {
      batches.delete
      ignoredTestConfigurations.delete
      tests.delete
      jenkinsConfiguration.delete
      teamCityConfiguration.delete
      systemConfiguration.delete
      initialiseConfiguration()
    }

    logger.info("Deleted all data")
  }

  private def initialiseConfiguration() {
    systemConfiguration.insert(SystemConfiguration())
    jenkinsConfiguration.insert(JenkinsConfiguration())
    teamCityConfiguration.insert(TeamCityConfiguration())
  }

  // Delete executions in batches to avoid performance issues on larger data sets
  private def deleteAllExecutions() {
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
    }
  }

  /**
   *  Strip out null chars, since Postgres throws an error:
   *    "ERROR: invalid byte sequence for encoding "UTF8": 0x00"
   */
  protected def removeNullChars(s: String) = s.filterNot(_ == '\u0000')

  def getSystemConfiguration(): SystemConfiguration =
    systemConfiguration.firstOption.getOrElse(throw new IllegalStateException("No system configuration present"))

  def updateSystemConfiguration(newConfig: SystemConfiguration) =
    systemConfiguration.update(newConfig)

  protected val configurationsCache: Cache[Seq[Configuration]] = Cache { rawGetConfigurations() }

  def getConfigurations(): Seq[Configuration] = configurationsCache.get

  private def rawGetConfigurations(): Seq[Configuration] =
    executions.groupBy(_.configuration).map(_._1).sorted.run

}