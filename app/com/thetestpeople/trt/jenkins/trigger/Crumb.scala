package com.thetestpeople.trt.jenkins.trigger

import play.api.libs.json._
import play.api.libs.json.Json._

object Crumb {

  def fromJson(json: JsValue): Crumb = {
    val crumb = (json \ "crumb").as[String]
    val crumbRequestField = (json \ "crumbRequestField").as[String]
    Crumb(crumb, crumbRequestField)
  }

}

case class Crumb(crumb: String, crumbRequestField: String = ".crumb") {

  def toParamMap = Map(crumbRequestField -> Seq(crumb))

}
