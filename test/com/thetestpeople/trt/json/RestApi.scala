package com.thetestpeople.trt.json

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.duration._
import com.thetestpeople.trt.json.JsonSerializers._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service.Incoming
import com.thetestpeople.trt.utils.UriUtils._
import play.api.Play.current
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.Call
import play.api.libs.ws.WSClient

object RestApi {

  private val Timeout = 20.seconds

}

case class RestApi(siteUrl: URI, client: WSClient = WS.client) {

  import RestApi._

  def addBatch(batch: Incoming.Batch): Id[Batch] = {
    val batchJson = Json.toJson(batch)
    val call: Call = controllers.routes.JsonController.addBatch()
    val future = client.url((siteUrl / call.url).toString).post(batchJson)
    val response = Await.result(future, Timeout)
    if (response.status == 200)
      response.json.as[Id[Batch]]
    else
      throw new RuntimeException(s"Problem adding batch: ${response.statusText}\n${response.body}")
  }

  def deleteAll() {
    val call: Call = controllers.routes.JsonController.deleteAll()
    val future = client.url((siteUrl / call.url).toString).post("")
    Await.result(future, Timeout)
  }

  def analyseAllExecutions() {
    val call: Call = controllers.routes.Application.analyseAllExecutions()
    val future = client.url((siteUrl / call.url).toString).post("")
    Await.result(future, Timeout)
  }
  
}