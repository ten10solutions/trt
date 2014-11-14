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

/**
 * Controller for the Tests screen.
 */
class TestsController(service: Service) extends AbstractController(service) with HasLogger {

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
    val selectedTestIds = getFormParameters("selectedTest").flatMap(Id.parse[Test])
    service.markTestsAsDeleted(selectedTestIds)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked tests as deleted.")
  }

  def undeleteTests() = Action { implicit request ⇒
    val selectedTestIds = getFormParameters("selectedTest").flatMap(Id.parse[Test])
    service.markTestsAsDeleted(selectedTestIds, deleted = false)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked tests as no longer deleted.")
  }

}