package com.thetestpeople.trt.jenkins.trigger

import play.api.libs.json._
import play.api.libs.json.Json._

/**
 * A parameter for a parameterised Jenkins build, after any variable substitutions have been made
 */
case class BuildParameter(param: String, value: String) {

  def asJson = Json.obj("name" -> param, "value" -> value)
}

