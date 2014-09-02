package com.thetestpeople.trt.jenkins.trigger

import java.net.URI

sealed trait TriggerResult {

  def successful: Boolean = false

}

object TriggerResult {

  case class Success(jobUrl: URI) extends TriggerResult { override def successful = true }
  case class AuthenticationProblem(message: String) extends TriggerResult
  case class ParameterProblem(param: String) extends TriggerResult
  case class OtherProblem(message: String, exceptionOpt: Option[Exception] = None) extends TriggerResult

}
