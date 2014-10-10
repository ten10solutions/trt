package controllers

import org.joda.time.Duration

import com.thetestpeople.trt.model.SystemConfiguration
import com.thetestpeople.trt.utils.FormUtils._

import play.api.data.Forms._
import play.api.data.Forms
import play.api.data.Form
import controllers.SystemConfigurationFormConstraints._

object SystemConfigurationForm {

  val form: Form[SystemConfiguration] =
    Form(mapping(
      "brokenDurationThreshold" -> duration.verifying(isNonNegativeDuration),
      "brokenCountThreshold" -> number.verifying(isNonNegative),
      "healthyDurationThreshold" -> duration.verifying(isNonNegativeDuration),
      "healthyCountThreshold" -> number.verifying(isNonNegative))(SystemConfiguration.apply)(SystemConfiguration.unapply))

}