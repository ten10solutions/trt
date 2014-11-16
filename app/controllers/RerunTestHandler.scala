package controllers

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.jenkins.trigger.TriggerResult
import controllers.jenkins._
import viewModel._
import scala.concurrent.Future
import play.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.data.Form
import com.thetestpeople.trt.importer.jenkins._
import com.thetestpeople.trt.importer._
import routes.ImportSpecController
import views.html

trait RerunTestHandler { self: AbstractController ⇒

  protected def rerunTests(testIds: Seq[Id[Test]])(implicit request: Request[AnyContent]) = {
    val triggerResult = service.rerunTests(testIds)

    val redirectTarget = previousUrlOpt.getOrElse(routes.TestsController.tests())

    triggerResult match {
      case TriggerResult.Success(jobUrl) ⇒
        Redirect(redirectTarget).flashing(
          "success" -> s"Triggered Jenkins build for ${testIds.size} ${if (testIds.size == 1) "test" else "tests"}",
          "link" -> jobUrl.toString)
      case TriggerResult.AuthenticationProblem(message) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"Could not trigger Jenkins build because of an authentication problem: $message. Check your Jenkins configuration:",
          "link" -> routes.JenkinsController.auth.url)
      case TriggerResult.ParameterProblem(param) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"Could not trigger Jenkins build because of a problem with parameter '$param'. Check your Jenkins configuration:",
          "link" -> routes.JenkinsController.reruns.url)
      case TriggerResult.OtherProblem(message, _) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"There was a problem triggering Jenkins build: $message")
    }
  }

}