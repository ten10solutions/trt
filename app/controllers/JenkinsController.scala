package controllers

import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.HasLogger

import play.api.data.Form
import play.api.mvc._
import routes.ImportSpecController
import viewModel._
import views.html
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

/**
 * Controller for Jenkins config screen
 */
class JenkinsController(service: Service) extends AbstractController(service) with HasLogger {

  def auth() = Action { implicit request ⇒
    Ok(html.jenkinsAuth(getJenkinsConfigurationForm))
  }

  def reruns() = Action { implicit request ⇒
    Ok(html.jenkinsReruns(getJenkinsConfigurationForm))
  }

  def updateAuth() = Action { implicit request ⇒
    JenkinsConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒ {
        logger.debug("updateAuth() errors: " + formWithErrors.errorsAsJson)
        BadRequest(html.jenkinsAuth(formWithErrors))
      },
      jenkinsConfiguration ⇒ {
        service.updateJenkinsConfiguration(jenkinsConfiguration.asJenkinsConfiguration)
        Redirect(routes.JenkinsController.auth).flashing("success" -> "Updated configuration")
      })
  }

  def updateReruns() = Action { implicit request ⇒
    JenkinsConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.jenkinsReruns(formWithErrors)),
      jenkinsConfiguration ⇒ {
        service.updateJenkinsConfiguration(jenkinsConfiguration.asJenkinsConfiguration)
        Redirect(routes.JenkinsController.reruns).flashing("success" -> "Updated configuration")
      })
  }

  private def getJenkinsConfigurationForm: Form[EditableJenkinsConfiguration] = {
    val editableConfig = EditableJenkinsConfiguration(service.getJenkinsConfiguration)
    JenkinsConfigurationForm.form.fill(editableConfig)
  }

}