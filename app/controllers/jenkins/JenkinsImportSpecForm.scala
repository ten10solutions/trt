package controllers.jenkins

import com.thetestpeople.trt.utils.FormUtils._
import play.api.data._
import play.api.data.Forms._
import viewModel._
import controllers.jenkins.JenkinsFormConstraints._
import com.thetestpeople.trt.model.Configuration

object CiImportSpecForm {

  lazy val form: Form[EditableImportSpec] =
    Form(Forms.mapping(
      "jobUrl" -> url.verifying(isCiJob),
      "pollingInterval" -> duration,
      "importConsoleLog" -> boolean,
      "configuration" -> optional(configuration))(EditableImportSpec.apply)(EditableImportSpec.unapply))

  lazy val initial: Form[EditableImportSpec] =
    form.bind(Map(
      "pollingInterval" -> "5 minutes",
      "configuration" -> Configuration.Default.configuration)).discardingErrors

}