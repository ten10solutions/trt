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
import com.thetestpeople.trt.jenkins.trigger.TriggerResult

/**
 * Controller for the Tests screen.
 */
class TestsController(service: Service) extends AbstractController(service) with RerunTestHandler with HasLogger {

  def tests(
    configurationOpt: Option[Configuration],
    testStatusOpt: Option[TestStatus],
    ignoredOpt: Option[Boolean],
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
            Ok(handleTests(testStatusOpt, ignoredOpt, configuration, nameOpt, groupOpt, categoryOpt, pagination, sortOpt, descendingOpt))
        }
    }
  }

  private def handleTests(
    testStatusOpt: Option[TestStatus],
    ignoredOpt: Option[Boolean],
    configuration: Configuration,
    nameOpt: Option[String],
    groupOpt: Option[String],
    categoryOpt: Option[String],
    pagination: Pagination,
    sortOpt: Option[Sort],
    descendingOpt: Option[Boolean])(implicit request: Request[_]) = {
    val sortBy = SortHelper.getTestSortBy(sortOpt, descendingOpt)
    val TestsInfo(tests, testCounts, ignoredTests) = service.getTests(
      configuration = configuration,
      testStatusOpt = testStatusOpt,
      ignoredOpt = ignoredOpt,
      nameOpt = nameOpt,
      groupOpt = groupOpt,
      categoryOpt = categoryOpt,
      startingFrom = pagination.firstItem,
      limit = pagination.pageSize,
      sortBy = sortBy)

    val testViews = tests.map(t ⇒ TestView(t, isIgnoredInConfiguration = ignoredTests contains t.id))
    val testsSummary = TestsSummaryView(configuration, testCounts)
    val paginationData = pagination.paginationData(testCounts.countFor(testStatusOpt))
    views.html.tests(testsSummary, testViews, configuration, testStatusOpt, ignoredOpt, nameOpt, groupOpt, categoryOpt,
      service.canRerun, paginationData, sortOpt, descendingOpt)
  }

  private def getSelectedTestIds(implicit request: Request[AnyContent]): Seq[Id[Test]] =
    getFormParameters("selectedTest").flatMap(Id.parse[Test])

  def deleteTests() = Action { implicit request ⇒
    service.markTestsAsDeleted(getSelectedTestIds)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked tests as deleted.")
  }

  def undeleteTests() = Action { implicit request ⇒
    service.markTestsAsDeleted(getSelectedTestIds, deleted = false)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked tests as no longer deleted.")
  }

  def rerunSelectedTests() = Action { implicit request ⇒
    rerunTests(getSelectedTestIds)
  }

}