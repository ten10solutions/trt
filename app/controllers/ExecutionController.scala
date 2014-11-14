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
 * Controller for the Execution screen.
 */
class ExecutionController(service: Service) extends AbstractController(service) with HasLogger {

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
          Redirect(routes.ExecutionController.execution(executionId)).flashing("success" -> "Comment updated.")
        else
          NotFound(s"Could not find test execution with id '$executionId'")
      case None ⇒
        BadRequest("No 'text' parameter provided'")
    }
  }

}