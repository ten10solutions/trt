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

class Application(service: Service, adminService: AdminService) extends Controller with HasLogger {

  import Application._

  private implicit def globalViewContext: GlobalViewContext = ControllerHelper.globalViewContext(service)

  def index = Action {
    Redirect(routes.Application.configurations())
  }

  def searchLogs(queryOpt: Option[String], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    logger.debug(s"searchLogs($queryOpt, page = $pageOpt, pageSize = $pageSizeOpt)")
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

  def configurations() = Action { implicit request ⇒
    logger.debug(s"configurations()")

    val testsSummaries =
      service.getTestCountsByConfiguration().map {
        case (configuration, testCounts) ⇒ TestsSummaryView(configuration, testCounts)
      }.toList.sortBy(_.configuration)

    Ok(views.html.configurations(testsSummaries))
  }

  def execution(executionId: Id[Execution]) = Action { implicit request ⇒
    logger.debug(s"execution($executionId)")
    service.getExecution(executionId) match {
      case None ⇒
        NotFound(s"Could not find test execution with id '$executionId'")
      case Some(execution) ⇒
        val executionView = new ExecutionView(execution)
        Ok(views.html.execution(executionView))
    }
  }

  def test(testId: Id[Test], configurationOpt: Option[Configuration], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    logger.debug(s"test($testId, configuration = $configurationOpt, page = $pageOpt, pageSize = $pageSizeOpt)")
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        val configuration = configurationOpt.getOrElse(Configuration.Default)
        handleTest(testId, configuration, pagination) match {
          case None       ⇒ NotFound(s"Could not find test with id '$testId'")
          case Some(html) ⇒ Ok(html)
        }
    }
  }

  private def handleTest(testId: Id[Test], configuration: Configuration, pagination: Pagination)(implicit request: Request[_]) =
    service.getTestAndExecutions(testId, configuration) map {
      case TestAndExecutions(test, executions, otherConfigurations) ⇒
        val executionViews = executions.map(e ⇒ ExecutionView(e))
        val testView = new TestView(test)
        val paginationData = pagination.paginationData(executions.size)
        views.html.test(testView, executionViews, Some(configuration), otherConfigurations, service.canRerun, paginationData)
    }

  def batch(batchId: Id[Batch], passedFilterOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    logger.debug(s"batch($batchId, passedFilterOpt = $passedFilterOpt, page = $pageOpt, pageSize = $pageSizeOpt)")
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
      case BatchAndExecutions(batch, executions, logOpt, importSpecIdOpt, commentOpt) ⇒
        val batchView = new BatchView(batch, executions, logOpt, importSpecIdOpt, commentOpt)
        val paginationData = pagination.paginationData(executions.size)
        val canRerun = service.canRerun
        views.html.batch(batchView, passedFilterOpt, canRerun, paginationData)
    }

  def batchLog(batchId: Id[Batch]) = Action { implicit request ⇒
    logger.debug(s"batchLog($batchId)")
    service.getBatchAndExecutions(batchId, None) match {
      case None ⇒
        NotFound(s"Could not find batch with id '$batchId'")
      case Some(BatchAndExecutions(batch, executions, logOpt, importSpecIdOpt, commentOpt)) ⇒
        logOpt match {
          case Some(log) ⇒
            val batchView = new BatchView(batch, List(), logOpt, importSpecIdOpt, commentOpt)
            Ok(views.html.batchLog(batchView, log))
          case None ⇒
            NotFound(s"Batch $batchId does not have an associated log recorded")
        }
    }
  }

  def batches(jobIdOpt: Option[Id[JenkinsJob]], configurationOpt: Option[Configuration], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    logger.debug(s"batches(jobId = $jobIdOpt, configuration = $configurationOpt, page = $pageOpt, pageSize = $pageSizeOpt)")
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒ BadRequest(errorMessage)
      case Right(pagination)  ⇒ Ok(handleBatches(jobIdOpt, configurationOpt, pagination))
    }
  }

  private def handleBatches(jobIdOpt: Option[Id[JenkinsJob]], configurationOpt: Option[Configuration], pagination: Pagination)(implicit request: Request[_]) = {
    val batches = service.getBatches(jobIdOpt, configurationOpt).map(new BatchView(_))
    val jobs = service.getJenkinsJobs()
    val paginationData = pagination.paginationData(batches.size)
    val hideChartInitially = batches.size >= HideBatchChartThreshold
    views.html.batches(batches.toList, jobIdOpt, configurationOpt, jobs, paginationData, hideChartInitially)
  }

  def executions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    logger.debug(s"executions(configuration = $configurationOpt, result = $resultOpt page = $pageOpt, pageSize = $pageSizeOpt)")
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

  private def getSelectedBatchIds(request: Request[AnyContent]): List[Id[Batch]] =
    for {
      requestMap ← request.body.asFormUrlEncoded.toList
      selectedIds ← requestMap.get("selectedBatch").toList
      idString ← selectedIds
      id ← Id.parse[Batch](idString)
    } yield id

  def deleteTests() = Action { implicit request ⇒
    val selectedTestIds = ControllerHelper.getSelectedTestIds(request)
    logger.debug(s"deleteTests(${selectedTestIds.mkString(",")})")

    service.markTestsAsDeleted(selectedTestIds)

    val redirectTarget = previousUrlOpt.getOrElse(routes.Application.configurations())
    Redirect(redirectTarget).flashing("success" -> "Marked tests as deleted.")
  }

  def deleteTest(id: Id[Test]) = Action { implicit request ⇒
    logger.debug(s"deleteTest($id)")

    service.markTestsAsDeleted(Seq(id))

    val redirectTarget = previousUrlOpt.getOrElse(routes.Application.configurations())
    Redirect(redirectTarget).flashing("success" -> "Marked test as deleted.")
  }

  def undeleteTest(id: Id[Test]) = Action { implicit request ⇒
    logger.debug(s"undeleteTest($id)")

    service.markTestsAsDeleted(Seq(id), deleted = false)

    val redirectTarget = previousUrlOpt.getOrElse(routes.Application.configurations())
    Redirect(redirectTarget).flashing("success" -> "Marked test as no longer deleted.")
  }

  def deleteBatches() = Action { implicit request ⇒
    val batchIds = getSelectedBatchIds(request)
    logger.debug(s"deleteBatches($batchIds)")

    service.deleteBatches(batchIds)

    val redirectTarget = previousUrlOpt.getOrElse(routes.Application.batches())
    val successMessage = deleteBatchesSuccessMessage(batchIds)
    Redirect(redirectTarget).flashing("success" -> successMessage)
  }

  def deleteBatch(batchId: Id[Batch]) = Action { implicit request ⇒
    logger.debug(s"deleteBatch($batchId)")
    val batchIds = List(batchId)
    service.deleteBatches(batchIds)

    val successMessage = deleteBatchesSuccessMessage(batchIds)
    Redirect(routes.Application.batches()).flashing("success" -> successMessage)
  }

  private def deleteBatchesSuccessMessage(batchIds: List[Id[Batch]]): String = {
    val batchWord = if (batchIds.size == 1) "batch" else "batches"
    s"Deleted ${batchIds.size} $batchWord"
  }

  def tests(
    configurationOpt: Option[Configuration],
    testStatusOpt: Option[TestStatus],
    nameOpt: Option[String],
    groupOpt: Option[String],
    pageOpt: Option[Int],
    pageSizeOpt: Option[Int],
    sortOpt: Option[Sort],
    descendingOpt: Option[Boolean]) = Action { implicit request ⇒
    Utils.time("Application.tests()") {
      logger.debug(s"tests(configuration = $configurationOpt, status = $testStatusOpt, name = $nameOpt, group = $groupOpt, page = $pageOpt, pageSize = $pageSizeOpt, sort = $sortOpt, descending = $descendingOpt)")
      Pagination.validate(pageOpt, pageSizeOpt) match {
        case Left(errorMessage) ⇒
          BadRequest(errorMessage)
        case Right(pagination) ⇒
          val configuration = configurationOpt.getOrElse(Configuration.Default)
          Ok(handleTests(testStatusOpt, configuration, nameOpt, groupOpt, pagination, sortOpt, descendingOpt))
      }
    }
  }

  private def getTestSortBy(sortOpt: Option[Sort], descendingOpt: Option[Boolean]): SortBy.Test = sortOpt match {
    case Some(Sort.Weather) ⇒ SortBy.Test.Weather(descendingOpt getOrElse false)
    case Some(Sort.Group)   ⇒ SortBy.Test.Group(descendingOpt getOrElse false)
    case _                  ⇒ SortBy.Test.Group(descending = false)
  }

  private def handleTests(testStatusOpt: Option[TestStatus], configuration: Configuration, nameOpt: Option[String], groupOpt: Option[String], pagination: Pagination, sortOpt: Option[Sort], descendingOpt: Option[Boolean])(implicit request: Request[_]) = {
    val sortBy = getTestSortBy(sortOpt, descendingOpt)
    val (testCounts, tests) = service.getTests(
      configuration = configuration,
      testStatusOpt = testStatusOpt,
      nameOpt = nameOpt,
      groupOpt = groupOpt,
      startingFrom = pagination.firstItem,
      limit = pagination.pageSize,
      sortBy = sortBy)

    val testViews = tests.map(new TestView(_))
    val testsSummary = TestsSummaryView(configuration, testCounts)
    val paginationData = pagination.paginationData(testCounts.countFor(testStatusOpt))
    views.html.tests(testsSummary, testViews.toList, configuration, testStatusOpt, nameOpt, groupOpt, service.canRerun, paginationData, sortOpt, descendingOpt)
  }

  def admin() = Action { implicit request ⇒
    logger.debug(s"admin()")
    Ok(views.html.admin())
  }

  def deleteAll() = Action { implicit request ⇒
    logger.debug(s"deleteAll()")
    adminService.deleteAll()
    Redirect(routes.Application.admin).flashing("success" -> "All data deleted")
  }

  def analyseAll() = Action { implicit request ⇒
    logger.debug("analyseAll()")
    adminService.analyseAll()
    Redirect(routes.Application.admin).flashing("success" -> "Analysis of all tests scheduled")
  }

  def updateSystemConfiguration() = Action { implicit request ⇒
    logger.debug(s"updateSystemConfiguration()")
    SystemConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(views.html.systemConfiguration(formWithErrors)),
      systemConfiguration ⇒ {
        service.updateSystemConfiguration(systemConfiguration)
        Redirect(routes.Application.editSystemConfiguration).flashing("success" -> "Updated configuration")
      })
  }

  def editSystemConfiguration() = Action { implicit request ⇒
    logger.debug(s"editSystemConfiguration()")
    val systemConfiguration = service.getSystemConfiguration
    val populatedForm = SystemConfigurationForm.form.fill(systemConfiguration)
    Ok(views.html.systemConfiguration(populatedForm))
  }

  private def previousUrlOpt(implicit request: Request[AnyContent]): Option[Call] =
    for {
      requestMap ← request.body.asFormUrlEncoded
      values ← requestMap.get("previousURL")
      previousUrl ← values.headOption
    } yield get(previousUrl)

  private def get(url: String) = new Call("GET", url)

  def testNames(query: String) = Action { implicit request ⇒
    logger.debug(s"testNames($query)")
    Ok(Json.toJson(service.getTestNames(query)))
  }

  def groups(query: String) = Action { implicit request ⇒
    logger.debug(s"groups($query)")
    Ok(Json.toJson(service.getGroups(query)))
  }

  def configurationChart(configuration: Configuration) = Action { implicit request ⇒
    logger.debug(s"configurationChart($configuration)")
    val counts = service.getHistoricalTestCounts().get(configuration).map(_.counts).getOrElse(List())
    Ok(Json.toJson(counts))
  }

  def analyseAllExecutions() = Action { implicit request ⇒
    logger.debug(s"analyseAllExecutions()")
    service.analyseAllExecutions()
    Ok(Json.toJson("OK"))
  }

  private def getDefaultConfiguration: Option[Configuration] = {
    val configurations = service.getConfigurations().sorted
    if (configurations.contains(Configuration.Default))
      Some(Configuration.Default)
    else
      configurations.headOption
  }

  def staleTests(configurationOpt: Option[Configuration], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    logger.debug(s"staleTests($configurationOpt, page = $pageOpt, pageSize = $pageSizeOpt)")
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        configurationOpt match {
          case None ⇒
            getDefaultConfiguration match {
              case None ⇒
                Redirect(routes.Application.configurations())
              case Some(defaultConfiguration) ⇒
                Redirect(routes.Application.staleTests(Some(defaultConfiguration)))
            }
          case Some(configuration) ⇒
            val (madOpt, tests) = service.staleTests(configuration)
            val pageTests = tests.drop(pagination.firstItem).take(pagination.pageSize)
            val testViews = pageTests.map(new TestView(_))
            val paginationData = pagination.paginationData(tests.size)
            val madViewOpt = madOpt.map(mad ⇒ MADView(mad))
            Ok(views.html.staleTests(madViewOpt, testViews, configuration, paginationData))
        }
    }
  }

  def setExecutionComment(executionId: Id[Execution]) = Action { implicit request ⇒
    logger.debug(s"setExecutionComment($executionId)")
    getText(request) match {
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

  private def getText(request: Request[AnyContent]): Option[String] =
    for {
      requestMap ← request.body.asFormUrlEncoded
      values ← requestMap.get("text")
      text ← values.headOption
    } yield text

  def setBatchComment(batchId: Id[Batch]) = Action { implicit request ⇒
    logger.debug(s"setBatchComment($batchId)")
    getText(request) match {
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

  def setTestComment(testId: Id[Test], configurationOpt: Option[Configuration]) = Action { implicit request ⇒
    logger.debug(s"setTestComment($testId)")
    getText(request) match {
      case Some(text) ⇒
        val result = service.setTestComment(testId, text)
        if (result)
          Redirect(routes.Application.test(testId, configurationOpt)).flashing("success" -> "Comment updated.")
        else
          NotFound(s"Could not find test with id '$testId'")
      case None ⇒
        BadRequest("No 'text' parameter provided'")
    }
  }

}
