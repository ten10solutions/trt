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
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

/**
 * Controller for miscellaneous screens
 */
class Application(service: Service) extends AbstractController(service) with HasLogger {

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

  //  Test API
  //  --------  
  def analyseAllExecutions() = Action { implicit request ⇒
    service.analyseAllExecutions()
    Ok(Json.toJson("OK"))
  }

}
