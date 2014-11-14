package controllers

import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.HasLogger

import play.api.data.Form
import play.api.mvc._
import routes.CiController
import viewModel._
import views.html

/**
 * Controller for TeamCity config screen
 */
class TeamCityController(service: Service) extends AbstractController(service) with HasLogger {

  def teamCityConfig() = Action { implicit request ⇒
    Ok(html.teamCityConfig(getTeamCityConfigurationForm))
  }

  def updateTeamCityConfig() = Action { implicit request ⇒
    TeamCityConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒ {
        logger.debug("updateTeamCityConfig() errors: " + formWithErrors.errorsAsJson)
        BadRequest(html.teamCityConfig(formWithErrors))
      },
      configuration ⇒ {
        val newConfig = configuration.asTeamCityConfiguration
        service.updateTeamCityConfiguration(newConfig)
        logger.debug(s"New TeamCity config: $newConfig")
        Redirect(routes.TeamCityController.teamCityConfig).flashing("success" -> "Updated configuration")
      })
  }

  private def getTeamCityConfigurationForm: Form[EditableTeamCityConfiguration] = {
    val editableConfig = EditableTeamCityConfiguration(service.getTeamCityConfiguration)
    TeamCityConfigurationForm.form.fill(editableConfig)
  }

}