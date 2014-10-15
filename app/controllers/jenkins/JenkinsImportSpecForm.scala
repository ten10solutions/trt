package controllers.jenkins

import com.thetestpeople.trt.utils.FormUtils._
import play.api.data._
import play.api.data.Forms._
import viewModel._
import controllers.jenkins.JenkinsFormConstraints._
import com.thetestpeople.trt.model.Configuration

object CiImportSpecForm {

  lazy val form: Form[EditableJenkinsImportData] =
    Form(Forms.mapping(
      "jobUrl" -> url.verifying(isJenkinsJob),
      "pollingInterval" -> duration,
      "importConsoleLog" -> boolean,
      "configuration" -> optional(configuration))(EditableJenkinsImportData.apply)(EditableJenkinsImportData.unapply))

  lazy val initial: Form[EditableJenkinsImportData] =
    form.bind(Map(
      "pollingInterval" -> "5 minutes",
      "configuration" -> Configuration.Default.configuration)).discardingErrors

}