package com.thetestpeople.trt.json

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.duration._
import com.thetestpeople.trt.json.JsonSerializers.idFormat
import com.thetestpeople.trt.json.JsonSerializers._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service.Incoming
import com.thetestpeople.trt.utils.UriUtils._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.Call

object RestApi {

  private val Timeout = 5.seconds

}

class RestApi(siteUrl: URI) {

  import RestApi._

  def addBatch(batch: Incoming.Batch): Id[Batch] = {
    val batchJson = Json.toJson(batch)
    val call: Call = controllers.routes.JsonController.addBatch()
    val future = WS.url((siteUrl / call.url).toString).post(batchJson)
    val response = Await.result(future, Timeout)
    response.json.as[Id[Batch]]
  }

  def deleteAll() {
    val call: Call = controllers.routes.JsonController.deleteAll()
    val future = WS.url((siteUrl / call.url).toString).post("")
    Await.result(future, Timeout)
  }

}