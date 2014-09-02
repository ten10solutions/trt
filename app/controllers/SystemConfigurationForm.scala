package controllers

import org.joda.time.Duration

import com.thetestpeople.trt.model.SystemConfiguration
import com.thetestpeople.trt.utils.FormUtils._

import play.api.data.Forms._
import play.api.data.Forms
import play.api.data.Form

object SystemConfigurationForm {

  val form: Form[SystemConfiguration] =
    Form(mapping(
      "failureDurationThreshold" -> duration,
      "failureCountThreshold" -> number,
      "passDurationThreshold" -> duration,
      "passCountThreshold" -> number)(SystemConfiguration.apply)(SystemConfiguration.unapply))

}