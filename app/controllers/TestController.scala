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
 * Controller for the Test screen.
 */
class TestController(service: Service) extends AbstractController(service) with HasLogger {

  import Application._

  def test(testId: Id[Test], configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        configurationOpt.orElse(getDefaultConfiguration) match {
          case None ⇒
            Redirect(routes.Application.index())
          case Some(configuration) ⇒
            handleTest(testId, configuration, resultOpt, pagination) match {
              case None       ⇒ NotFound(s"Could not find test with id '$testId'")
              case Some(html) ⇒ Ok(html)
            }
        }
    }
  }

  private def handleTest(testId: Id[Test], configuration: Configuration, resultOpt: Option[Boolean], pagination: Pagination)(implicit request: Request[_]) =
    service.getTestAndExecutions(testId, configuration, resultOpt) map {
      case TestAndExecutions(test, executions, otherConfigurations, categories) ⇒
        val executionViews = executions.map(e ⇒ ExecutionView(e))
        val testView = new TestView(test, categories)
        val paginationData = pagination.paginationData(executions.size)
        views.html.test(testView, executionViews, Some(configuration), resultOpt, otherConfigurations, service.canRerun, paginationData)
    }

  def setTestComment(testId: Id[Test], configurationOpt: Option[Configuration]) = Action { implicit request ⇒
    getFormParameter("text") match {
      case Some(text) ⇒
        val result = service.setTestComment(testId, text)
        if (result)
          Redirect(routes.TestController.test(testId, configurationOpt)).flashing("success" -> "Comment updated.")
        else
          NotFound(s"Could not find test with id '$testId'")
      case None ⇒
        BadRequest("No 'text' parameter provided'")
    }
  }

  def addCategory(testId: Id[Test]) = Action { implicit request ⇒
    getFormParameter("category") match {
      case Some(category) ⇒
        val result = service.addCategory(testId, category)
        result match {
          case AddCategoryResult.DuplicateCategory ⇒
            Redirect(previousUrlOrDefault).flashing("error" -> s"Test is already in the '$category' category.")
          case AddCategoryResult.NoTestFound ⇒
            BadRequest(s"No test found with id $testId")
          case AddCategoryResult.Success ⇒
            Redirect(previousUrlOrDefault).flashing("success" -> s"Test was added to the '$category' category.")
        }
      case None ⇒
        BadRequest("No 'category' parameter provided'")
    }
  }

  def removeCategory(testId: Id[Test]) = Action { implicit request ⇒
    getFormParameter("category") match {
      case Some(category) ⇒
        service.removeCategory(testId, category)
        Redirect(previousUrlOrDefault).flashing("success" -> s"Test was removed from the '$category' category.")
      case None ⇒
        BadRequest("No 'category' parameter provided'")
    }
  }

  def deleteTest(id: Id[Test]) = Action { implicit request ⇒
    service.markTestsAsDeleted(Seq(id))
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked test as deleted.")
  }

  def undeleteTest(id: Id[Test]) = Action { implicit request ⇒
    service.markTestsAsDeleted(Seq(id), deleted = false)
    Redirect(previousUrlOrDefault).flashing("success" -> "Marked test as no longer deleted.")
  }

}