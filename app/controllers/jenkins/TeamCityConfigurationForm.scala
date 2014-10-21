package controllers

import com.thetestpeople.trt.utils.FormUtils._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation._
import viewModel._
import controllers.jenkins.JenkinsFormConstraints._
import play.api.data.Forms
import com.thetestpeople.trt.utils.http.Credentials

object TeamCityConfigurationForm {

  val form: Form[EditableTeamCityConfiguration] = {
    val credentialsMapping = mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText)(Credentials.apply)(Credentials.unapply)
    val formMapping = mapping(
      "credentials" -> optional(credentialsMapping))(EditableTeamCityConfiguration.apply)(EditableTeamCityConfiguration.unapply)
    Form(formMapping)
  }

}
