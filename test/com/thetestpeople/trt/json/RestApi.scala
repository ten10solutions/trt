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
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.ning.NingAsyncHttpClientConfigBuilder
import play.api.libs.ws.DefaultWSClientConfig
import com.thetestpeople.trt.utils.WsUtils

object RestApi {

  private val Timeout = 20.seconds

}

/**
 * Access to the API for tests
 */
case class RestApi(siteUrl: URI, client: WSClient = WsUtils.newWsClient) {

  import RestApi._

  def addBatch(batch: Incoming.Batch): Id[Batch] = {
    val batchJson = Json.toJson(batch)
    val call: Call = controllers.routes.ApiController.addBatch()
    val future = client.url((siteUrl / call.url).toString).post(batchJson)
    val response = Await.result(future, Timeout)
    if (response.status == 200)
      response.json.as[Id[Batch]]
    else
      throw new RuntimeException(s"Problem adding batch: ${response.statusText}\n${response.body}")
  }

  def deleteAll() {
    val call: Call = controllers.routes.ApiController.deleteAll()
    val future = client.url((siteUrl / call.url).toString).post("")
    Await.result(future, Timeout)
  }

  def analyseAllExecutions() {
    val call: Call = controllers.routes.Application.analyseAllExecutions()
    val future = client.url((siteUrl / call.url).toString).post("")
    Await.result(future, Timeout)
  }

  def getTests(configurationOpt: Option[Configuration] = None, statusOpt: Option[TestStatus] = None): Seq[TestApiView] = {
    val call: Call = controllers.routes.ApiController.getTests(configurationOpt, statusOpt)
    val future = client.url((siteUrl / call.url).toString).get()
    val response = Await.result(future, Timeout)
    if (response.status == 200)
      response.json.as[Seq[TestApiView]]
    else
      throw new RuntimeException(s"Problem getting tests: ${response.statusText}\n${response.body}")
  }
}