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

object JenkinsConfigurationForm {

  val form: Form[EditableJenkinsConfiguration] = {
    val paramMapping = mapping(
      "name" -> nonEmptyText,
      "value" -> text)(JenkinsJobParam.apply)(JenkinsJobParam.unapply)
    val credentialsMapping = mapping(
      "username" -> nonEmptyText,
      "apiToken" -> nonEmptyText.verifying(isApiToken))(Credentials.apply)(Credentials.unapply)
    val formMapping = mapping(
      "credentials" -> optional(credentialsMapping),
      "rerunJobUrl" -> optional(url.verifying(isJenkinsJob)),
      "authenticationToken" -> optional(text),
      "params" -> seq(paramMapping))(EditableJenkinsConfiguration.apply)(EditableJenkinsConfiguration.unapply)
    Form(formMapping.verifying(parametersAreAllDistinct))
  }

}
