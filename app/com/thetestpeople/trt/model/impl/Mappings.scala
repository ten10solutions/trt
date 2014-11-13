package com.thetestpeople.trt.model.impl

import org.joda.time._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import java.net.URI

trait Mappings { self: SlickDao ⇒

  import driver.simple._
  import jodaSupport._
  import Mappers._

  class AnalysisMapping(tag: Tag) extends Table[Analysis](tag, "analysis") {

    def testId = column[Id[Test]]("test_id", O.NotNull)
    def configuration = column[Configuration]("configuration", O.NotNull)
    def status = column[TestStatus]("status", O.NotNull)
    def weather = column[Double]("weather", O.NotNull)
    def consecutiveFailures = column[Int]("consecutive_failures", O.NotNull)
    def failingSince = column[Option[DateTime]]("failing_since")
    def lastPassedExecutionId = column[Option[Id[Execution]]]("last_passed_execution_id")
    def lastPassedTime = column[Option[DateTime]]("last_passed_time")
    def lastFailedExecutionId = column[Option[Id[Execution]]]("last_failed_execution_id")
    def lastFailedTime = column[Option[DateTime]]("last_failed_time")
    def whenAnalysed = column[DateTime]("when_analysed", O.NotNull)
    def medianDuration = column[Option[Duration]]("median_duration")

    def * = (testId, configuration, status, weather, consecutiveFailures, failingSince, lastPassedExecutionId, lastPassedTime,
      lastFailedExecutionId, lastFailedTime, whenAnalysed, medianDuration) <> (Analysis.tupled, Analysis.unapply)

    def testIdFk = foreignKey("fk_analysis_test_id", testId, Tables.tests)(_.id)

    def lastPassedExecutionIdFk = foreignKey("fk_last_passed_execution_id", lastPassedExecutionId, Tables.executions)(_.id)

    def lastFailedExecutionIdFk = foreignKey("fk_last_failed_execution_id", lastFailedExecutionId, Tables.executions)(_.id)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (testId.?, configuration.?, status.?, weather.?, consecutiveFailures.?, failingSince, lastPassedExecutionId,
      lastPassedTime, lastFailedExecutionId, lastFailedTime, whenAnalysed.?, medianDuration).shaped <> ({ r ⇒
        import r._; _1.map(_ ⇒
          Analysis.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7, _8, _9, _10, _11.get, _12)))
      }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class BatchLogMapping(tag: Tag) extends Table[BatchLogRow](tag, "batch_logs") {

    def batchId = column[Id[Batch]]("batch_id", O.PrimaryKey, O.NotNull)
    def log = column[String]("log", O.NotNull)

    def batchIdFk = foreignKey("fk_batch_log_id", batchId, Tables.batches)(_.id)

    def * = (batchId, log) <> (BatchLogRow.tupled, BatchLogRow.unapply)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (batchId.?, log.?).shaped <> ({ r ⇒
      import r._; _1.map(_ ⇒ BatchLogRow.tupled((_1.get, _2.get)))
    }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class BatchMapping(tag: Tag) extends Table[Batch](tag, "batches") {

    import Mappers._

    def id = column[Id[Batch]]("id", O.PrimaryKey, O.NotNull, O.AutoInc)
    def url = column[Option[URI]]("url")
    def executionTime = column[DateTime]("execution_time", O.NotNull)
    def duration = column[Option[Duration]]("duration")
    def name = column[Option[String]]("name")
    def passed = column[Boolean]("passed", O.NotNull)
    def totalCount = column[Int]("total_count", O.NotNull)
    def passCount = column[Int]("pass_count", O.NotNull)
    def failCount = column[Int]("fail_count", O.NotNull)
    def configuration = column[Option[Configuration]]("configuration")

    def * = (id, url, executionTime, duration, name, passed, totalCount, passCount, failCount, configuration) <>
      (Batch.tupled, Batch.unapply)

  }

  class ExecutionLogMapping(tag: Tag) extends Table[ExecutionLogRow](tag, "execution_logs") {

    def executionId = column[Id[Execution]]("execution_id", O.PrimaryKey, O.NotNull)
    def log = column[String]("log", O.NotNull)

    def executionIdFk = foreignKey("fk_execution_log_id", executionId, Tables.executions)(_.id)

    def * = (executionId, log) <> (ExecutionLogRow.tupled, ExecutionLogRow.unapply)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (executionId.?, log.?).shaped <> ({ r ⇒
      import r._; _1.map(_ ⇒ ExecutionLogRow.tupled((_1.get, _2.get)))
    }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class ExecutionMapping(tag: Tag) extends Table[Execution](tag, "executions") {

    def id = column[Id[Execution]]("id", O.PrimaryKey, O.NotNull, O.AutoInc)
    def batchId = column[Id[Batch]]("batch_id", O.NotNull)
    def testId = column[Id[Test]]("test_id", O.NotNull)
    def executionTime = column[DateTime]("execution_time", O.NotNull)
    def duration = column[Option[Duration]]("duration")
    def summary = column[Option[String]]("summary")
    def passed = column[Boolean]("passed", O.NotNull)
    def configuration = column[Configuration]("configuration")

    def * = (id, batchId, testId, executionTime, duration, summary, passed, configuration) <>
      (Execution.tupled, Execution.unapply)

    def testFk = foreignKey("test_id_fk", testId, Tables.tests)(_.id)
    def batchFk = foreignKey("batch_execution_id_fk", batchId, Tables.batches)(_.id)

  }

  class CiJobMapping(tag: Tag) extends Table[CiJob](tag, "ci_jobs") {

    def id = column[Id[CiJob]]("ID", O.PrimaryKey, O.NotNull, O.AutoInc)
    def url = column[URI]("url", O.NotNull)
    def name = column[String]("name", O.NotNull)

    def * = (id, url, name) <> (CiJob.tupled, CiJob.unapply)

    def urlIndex = index("idx_jenkins_job_url", url)

  }

  class CiBuildMapping(tag: Tag) extends Table[CiBuild](tag, "ci_builds") {

    def batchId = column[Id[Batch]]("batch_id", O.PrimaryKey, O.NotNull)
    def importTime = column[DateTime]("import_time", O.NotNull)
    def buildUrl = column[URI]("build_url", O.NotNull)
    def buildNumber = column[Option[Int]]("build_number")
    def buildName = column[Option[String]]("build_name")
    def jobId = column[Id[CiJob]]("job_id", O.NotNull)
    def importSpecId = column[Option[Id[CiImportSpec]]]("import_spec_id")

    def * = (batchId, importTime, buildUrl, buildNumber, buildName, jobId, importSpecId) <> (CiBuild.tupled, CiBuild.unapply)

    def buildUrlIndex = index("idx_jenkins_build_url", buildUrl, unique = true)
    def jobIdIndex = index("idx_jenkins_job_id", jobId)
    def batchIdFk = foreignKey("fk_batch_id", batchId, Tables.batches)(_.id)
    def jobIdFk = foreignKey("fk_job_id", jobId, Tables.ciJobs)(_.id)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (batchId.?, importTime.?, buildUrl.?, buildNumber, buildName, jobId.?, importSpecId).shaped <> ({ r ⇒
      import r._; _1.map(_ ⇒
        CiBuild.tupled((_1.get, _2.get, _3.get, _4, _5, _6.get, _7)))
    }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class JenkinsConfigurationMapping(tag: Tag) extends Table[JenkinsConfiguration](tag, "jenkins_configuration") {

    def usernameOpt = column[Option[String]]("username")
    def apiTokenOpt = column[Option[String]]("api_token")
    def rerunJobUrlOpt = column[Option[URI]]("rerun_job_url")
    def authenticationTokenOpt = column[Option[String]]("authentication_token")
    def * = (usernameOpt, apiTokenOpt, rerunJobUrlOpt, authenticationTokenOpt) <> (JenkinsConfiguration.tupled, JenkinsConfiguration.unapply)

  }

  class TeamCityConfigurationMapping(tag: Tag) extends Table[TeamCityConfiguration](tag, "teamcity_configuration") {

    def usernameOpt = column[Option[String]]("username")
    def passwordOpt = column[Option[String]]("password")

    def * = (usernameOpt, passwordOpt) <> (TeamCityConfiguration.tupled, TeamCityConfiguration.unapply)

  }

  class JenkinsJobParamMapping(tag: Tag) extends Table[JenkinsJobParam](tag, "jenkins_job_params") {

    def paramName = column[String]("param_name")
    def value = column[String]("value")
    def * = (paramName, value) <> (JenkinsJobParam.tupled, JenkinsJobParam.unapply)

  }

  class CiImportSpecMapping(tag: Tag) extends Table[CiImportSpec](tag, "ci_import_specs") {

    def id = column[Id[CiImportSpec]]("id", O.PrimaryKey, O.NotNull, O.AutoInc)
    def ciType = column[CiType]("ci_type", O.NotNull)
    def jobUrl = column[URI]("job_url", O.NotNull)
    def pollingInterval = column[Duration]("polling_interval", O.NotNull)
    def importConsoleLog = column[Boolean]("import_console_log", O.NotNull)
    def lastChecked = column[Option[DateTime]]("last_checked")
    def configuration = column[Option[Configuration]]("configuration")

    def * = (id, ciType, jobUrl, pollingInterval, importConsoleLog, lastChecked, configuration) <> (CiImportSpec.tupled, CiImportSpec.unapply)

  }

  class SystemConfigurationMapping(tag: Tag) extends Table[SystemConfiguration](tag, "system_configuration") {

    val projectName = column[Option[String]]("project_name")
    val failureDurationThreshold = column[Duration]("failure_duration_threshold", O.NotNull)
    val failureCountThreshold = column[Int]("failure_count_threshold", O.NotNull)
    val passDurationThreshold = column[Duration]("pass_duration_threshold", O.NotNull)
    val passCountThreshold = column[Int]("pass_count_threshold", O.NotNull)

    def * = (projectName, failureDurationThreshold, failureCountThreshold, passDurationThreshold, passCountThreshold) <>
      (SystemConfiguration.tupled, SystemConfiguration.unapply)

  }

  class TestMapping(tag: Tag) extends Table[Test](tag, "tests") {

    def id = column[Id[Test]]("id", O.PrimaryKey, O.NotNull, O.AutoInc)
    def name = column[String]("name", O.NotNull)
    def group = column[Option[String]]("group")
    def deleted = column[Boolean]("deleted")
    def * = (id, name, group, deleted) <> (Test.tupled, Test.unapply)

    def nameGroupIndex = index("idx_test_name_group", (name, group), unique = true)

  }

  class ExecutionCommentMapping(tag: Tag) extends Table[ExecutionComment](tag, "execution_comments") {

    def executionId = column[Id[Execution]]("execution_id", O.PrimaryKey, O.NotNull)
    def text = column[String]("comment", O.NotNull)

    def * = (executionId, text) <> (ExecutionComment.tupled, ExecutionComment.unapply)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (executionId.?, text.?).shaped <> ({ r ⇒
      import r._; _1.map(_ ⇒ ExecutionComment.tupled((_1.get, _2.get)))
    }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class BatchCommentMapping(tag: Tag) extends Table[BatchComment](tag, "batch_comments") {

    def batchId = column[Id[Batch]]("batch_id", O.PrimaryKey, O.NotNull)
    def text = column[String]("comment", O.NotNull)

    def * = (batchId, text) <> (BatchComment.tupled, BatchComment.unapply)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (batchId.?, text.?).shaped <> ({ r ⇒
      import r._; _1.map(_ ⇒ BatchComment.tupled((_1.get, _2.get)))
    }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class TestCommentMapping(tag: Tag) extends Table[TestComment](tag, "test_comments") {

    def testId = column[Id[Test]]("test_id", O.PrimaryKey, O.NotNull)
    def text = column[String]("comment", O.NotNull)

    def * = (testId, text) <> (TestComment.tupled, TestComment.unapply)

    /**
     * (Autogenerated) helper code for outer joins
     */
    def ? = (testId.?, text.?).shaped <> ({ r ⇒
      import r._; _1.map(_ ⇒ TestComment.tupled((_1.get, _2.get)))
    }, (_: Any) ⇒ throw new Exception("Inserting into ? projection not supported."))

  }

  class TestCategoryMapping(tag: Tag) extends Table[TestCategory](tag, "test_categories") {

    def testId = column[Id[Test]]("test_id", O.PrimaryKey, O.NotNull)
    def category = column[String]("category", O.NotNull)
    def isUserCategory = column[Boolean]("is_user_category", O.NotNull)

    def * = (testId, category, isUserCategory) <> (TestCategory.tupled, TestCategory.unapply)

  }

}