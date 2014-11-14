package controllers

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.model.jenkins._
import play.Logger
import play.api.mvc._
import viewModel._
import java.net.URI
import play.api.libs.json._
import com.thetestpeople.trt.json.JsonSerializers._

object Application {

  val HideBatchChartThreshold = 1000

}

class Application(service: Service) extends AbstractController(service) with HasLogger {

  import Application._

  def index = Action {
    Redirect(routes.Application.configurations())
  }

  // Search logs screen
  // ------------------
  
  def searchLogs(queryOpt: Option[String], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt, defaultPageSize = 8) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        val (executionViews, totalHits) = queryOpt match {
          case Some(query) ⇒
            val (executionAndFragments, totalHits) = service.searchLogs(query, startingFrom = pagination.firstItem, limit = pagination.pageSize)
            (executionAndFragments.map(ExecutionView.fromExecutionAndFragment), totalHits)
          case None ⇒ (Seq(), 0)
        }
        val paginationData = pagination.paginationData(totalHits)
        Ok(views.html.searchLogs(queryOpt = queryOpt, executionViews, paginationData))
    }
  }

  // Configurations screen
  // ---------------------
  
  def configurations() = Action { implicit request ⇒
    val testsSummaries =
      service.getTestCountsByConfiguration().map {
        case (configuration, testCounts) ⇒ TestsSummaryView(configuration, testCounts)
      }.toSeq.sortBy(_.configuration)
    val historicalIntervalOpt = service.getAllHistoricalTestCounts.interval
    Ok(views.html.configurations(testsSummaries, ConfigurationsView(historicalIntervalOpt)))
  }

  // Execution screen
  // ----------------

  def execution(executionId: Id[Execution]) = Action { implicit request ⇒
    service.getExecution(executionId) match {
      case None ⇒
        NotFound(s"Could not find test execution with id '$executionId'")
      case Some(execution) ⇒
        val executionView = new ExecutionView(execution)
        Ok(views.html.execution(executionView))
    }
  }

  def setExecutionComment(executionId: Id[Execution]) = Action { implicit request ⇒
    getFormParameter("text") match {
      case Some(text) ⇒
        val result = service.setExecutionComment(executionId, text)
        if (result)
          Redirect(routes.Application.execution(executionId)).flashing("success" -> "Comment updated.")
        else
          NotFound(s"Could not find test execution with id '$executionId'")
      case None ⇒
        BadRequest("No 'text' parameter provided'")
    }
  }

  // Batch screen
  // ------------

  def batch(batchId: Id[Batch], passedFilterOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        handleBatch(batchId, passedFilterOpt, pagination) match {
          case None       ⇒ NotFound(s"Could not find batch with id '$batchId'")
          case Some(html) ⇒ Ok(html)
        }
    }
  }

  private def handleBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean], pagination: Pagination)(implicit request: Request[_]) =
    service.getBatchAndExecutions(batchId, passedFilterOpt) map {
      case EnrichedBatch(batch, executions, logOpt, importSpecIdOpt, commentOpt) ⇒
        val batchView = new BatchView(batch, executions, logOpt, importSpecIdOpt, commentOpt)
        val paginationData = pagination.paginationData(executions.size)
        views.html.batch(batchView, passedFilterOpt, service.canRerun, paginationData)
    }

  def setBatchComment(batchId: Id[Batch]) = Action { implicit request ⇒
    getFormParameter("text") match {
      case Some(text) ⇒
        val result = service.setBatchComment(batchId, text)
        if (result)
          Redirect(routes.Application.batch(batchId)).flashing("success" -> "Comment updated.")
        else
          NotFound(s"Could not find batch with id '$batchId'")
      case None ⇒
        BadRequest("No 'text' parameter provided'")
    }
  }

  def deleteBatch(batchId: Id[Batch]) = Action { implicit request ⇒
    val batchIds = Seq(batchId)
    service.deleteBatches(batchIds)

    val successMessage = deleteBatchesSuccessMessage(batchIds)
    Redirect(routes.Application.batches()).flashing("success" -> successMessage)
  }

  private def deleteBatchesSuccessMessage(batchIds: Seq[Id[Batch]]): String = {
    val batchWord = if (batchIds.size == 1) "batch" else "batches"
    s"Deleted ${batchIds.size} $batchWord"
  }

  // Batch log screen
  // ----------------

  def batchLog(batchId: Id[Batch]) = Action { implicit request ⇒
    service.getBatchAndExecutions(batchId, None) match {
      case None ⇒
        NotFound(s"Could not find batch with id '$batchId'")
      case Some(EnrichedBatch(batch, executions, logOpt, importSpecIdOpt, commentOpt)) ⇒
        logOpt match {
          case Some(log) ⇒
            val batchView = new BatchView(batch, Seq(), logOpt, importSpecIdOpt, commentOpt)
            Ok(views.html.batchLog(batchView, log))
          case None ⇒
            NotFound(s"Batch $batchId does not have an associated log recorded")
        }
    }
  }

  // Batches screen
  // --------------

  def batches(jobIdOpt: Option[Id[CiJob]], configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒ BadRequest(errorMessage)
      case Right(pagination)  ⇒ Ok(handleBatches(jobIdOpt, configurationOpt, resultOpt, pagination))
    }
  }

  private def handleBatches(jobIdOpt: Option[Id[CiJob]], configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pagination: Pagination)(implicit request: Request[_]) = {
    val batches = service.getBatches(jobIdOpt, configurationOpt, resultOpt).map(new BatchView(_))
    val jobs = service.getCiJobs()
    val paginationData = pagination.paginationData(batches.size)
    val hideChartInitially = batches.size >= HideBatchChartThreshold
    views.html.batches(batches, jobIdOpt, configurationOpt, resultOpt, jobs, paginationData, hideChartInitially)
  }

  def deleteBatches() = Action { implicit request ⇒
    val batchIds = getSelectedBatchIds(request)

    service.deleteBatches(batchIds)

    val redirectTarget = previousUrlOpt.getOrElse(routes.Application.batches())
    val successMessage = deleteBatchesSuccessMessage(batchIds)
    Redirect(redirectTarget).flashing("success" -> successMessage)
  }

  private def getSelectedBatchIds(implicit request: Request[AnyContent]): Seq[Id[Batch]] =
    getFormParameters("selectedBatch").flatMap(Id.parse[Batch])

  // Executions screen
  // -----------------

  def executions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒ BadRequest(errorMessage)
      case Right(pagination)  ⇒ Ok(handleExecutions(configurationOpt, resultOpt, pagination))
    }
  }

  private def handleExecutions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pagination: Pagination)(implicit request: Request[_]) = {
    val ExecutionsAndTotalCount(executions, totalExecutionCount) =
      service.getExecutions(configurationOpt, resultOpt, startingFrom = pagination.firstItem, limit = pagination.pageSize)
    val executionVolume = service.getExecutionVolume(configurationOpt)
    val paginationData = pagination.paginationData(totalExecutionCount)
    val executionViews = executions.map(new ExecutionView(_))
    views.html.executions(executionViews, totalExecutionCount, configurationOpt, resultOpt, paginationData, executionVolume)
  }

  // Deleted tests screen
  // --------------------

  def deletedTests(pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        val deletedTests = service.getDeletedTests()
        val testViews = deletedTests.drop(pagination.firstItem).take(pagination.pageSize).map(TestView(_))
        val paginationData = pagination.paginationData(deletedTests.size)
        Ok(views.html.deletedTests(testViews, paginationData))
    }
  }

  // Tests screen
  // ------------

  def tests(
    configurationOpt: Option[Configuration],
    testStatusOpt: Option[TestStatus],
    nameOpt: Option[String],
    groupOpt: Option[String],
    categoryOpt: Option[String],
    pageOpt: Option[Int],
    pageSizeOpt: Option[Int],
    sortOpt: Option[Sort],
    descendingOpt: Option[Boolean]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        configurationOpt.orElse(getDefaultConfiguration) match {
          case None ⇒
            Redirect(routes.Application.index())
          case Some(configuration) ⇒
            Ok(handleTests(testStatusOpt, configuration, nameOpt, groupOpt, categoryOpt, pagination, sortOpt, descendingOpt))
        }
    }
  }

  private def handleTests(
    testStatusOpt: Option[TestStatus],
    configuration: Configuration,
    nameOpt: Option[String],
    groupOpt: Option[String],
    categoryOpt: Option[String],
    pagination: Pagination,
    sortOpt: Option[Sort],
    descendingOpt: Option[Boolean])(implicit request: Request[_]) = {
    val sortBy = SortHelper.getTestSortBy(sortOpt, descendingOpt)
    val (testCounts, tests) = service.getTests(
      configuration = configuration,
      testStatusOpt = testStatusOpt,
      nameOpt = nameOpt,
      groupOpt = groupOpt,
      categoryOpt = categoryOpt,
      startingFrom = pagination.firstItem,
      limit = pagination.pageSize,
      sortBy = sortBy)

    val testViews = tests.map(TestView(_))
    val testsSummary = TestsSummaryView(configuration, testCounts)
    val paginationData = pagination.paginationData(testCounts.countFor(testStatusOpt))
    views.html.tests(testsSummary, testViews, configuration, testStatusOpt, nameOpt, groupOpt, categoryOpt, service.canRerun, paginationData, sortOpt, descendingOpt)
  }

  def deleteTests() = Action { implicit request ⇒
    val selectedTestIds = ControllerHelper.getSelectedTestIds(request)
    service.markTestsAsDeleted(selectedTestIds)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked tests as deleted.")
  }

  def undeleteTests() = Action { implicit request ⇒
    val selectedTestIds = ControllerHelper.getSelectedTestIds(request)
    service.markTestsAsDeleted(selectedTestIds, deleted = false)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked tests as no longer deleted.")
  }

  // System Configuration screen
  // ----------------------------

  def updateSystemConfiguration() = Action { implicit request ⇒
    SystemConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(views.html.systemConfiguration(formWithErrors)),
      systemConfiguration ⇒ {
        service.updateSystemConfiguration(systemConfiguration)
        Redirect(routes.Application.editSystemConfiguration).flashing("success" -> "Updated configuration")
      })
  }

  def editSystemConfiguration() = Action { implicit request ⇒
    val systemConfiguration = service.getSystemConfiguration
    val populatedForm = SystemConfigurationForm.form.fill(systemConfiguration)
    Ok(views.html.systemConfiguration(populatedForm))
  }

  //  Test API
  //  --------  
  def analyseAllExecutions() = Action { implicit request ⇒
    service.analyseAllExecutions()
    Ok(Json.toJson("OK"))
  }

  // State Tests screen
  // ------------------

  def staleTests(configurationOpt: Option[Configuration], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        configurationOpt.orElse(getDefaultConfiguration) match {
          case None ⇒
            Redirect(routes.Application.index())
          case Some(configuration) ⇒
            handleStaleTests(configuration, pagination)
        }
    }
  }

  private def handleStaleTests(configuration: Configuration, pagination: Pagination)(implicit request: Request[_]) = {
    val (madOpt, tests) = service.staleTests(configuration)
    val pageTests = tests.drop(pagination.firstItem).take(pagination.pageSize)
    val testViews = pageTests.map(new TestView(_))
    val paginationData = pagination.paginationData(tests.size)
    val madViewOpt = madOpt.map(mad ⇒ MADView(mad))
    Ok(views.html.staleTests(madViewOpt, testViews, configuration, paginationData))
  }

}
