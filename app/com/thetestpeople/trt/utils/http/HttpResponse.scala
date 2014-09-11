package com.thetestpeople.trt.utils.http

import scala.xml.Elem
import scala.xml.XML

import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class HttpResponse(status: Int, statusText: String, body: String = "") {

  def bodyAsJson: JsValue = Json.parse(body)

  def bodyAsXml: Elem =
    try
      XML.loadString(body)
    catch {
      case e: Exception ⇒
        val bodyStart = body.take(200)
        throw new HttpException(s"Problem parsing XML from response body: $bodyStart", e)
    }
  
}
