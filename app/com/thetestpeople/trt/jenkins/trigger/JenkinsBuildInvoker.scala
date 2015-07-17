package com.thetestpeople.trt.jenkins.trigger

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.duration._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.ws.WS
import play.api.libs.ws.WS._
import play.api.libs.ws.WSResponse

/**
 * @param authenticationTokenOpt -- build token for the Jenkins job (if required)
 * @param credentialsOpt -- username/api token to use with basic auth
 */
class JenkinsBuildInvoker(
  http: Http,
  jobUrl: URI,
  authenticationTokenOpt: Option[String] = None,
  credentialsOpt: Option[Credentials] = None)
    extends HasLogger {

  def triggerBuild(buildParameters: Seq[BuildParameter] = Nil, crumbOpt: Option[Crumb] = None): TriggerResult =
    try {
      val postUrl = jobUrl / "build"
      val postBody = constructPostBody(buildParameters, crumbOpt)
      val response = http.post(postUrl, basicAuthOpt = credentialsOpt, bodyParams = postBody)
      diagnoseResponse(response)
    } catch {
      case e: Exception ⇒
        logger.error("Problem triggering a build on Jenkins", e)
        TriggerResult.OtherProblem(s"$e.getMessage", Some(e))
    }

  private def diagnoseResponse(response: HttpResponse): TriggerResult = {
    logger.debug(s"Jenkins response: ${response.status} ${response.statusText}")
    response.status match {
      case 201       ⇒ TriggerResult.Success(jobUrl)
      case 401 | 403 ⇒ TriggerResult.AuthenticationProblem(s"${response.status} ${response.statusText}")
      case 500 ⇒ findNoSuchParamMessage(response.body) match {
        case Some(param) ⇒ TriggerResult.ParameterProblem(param)
        case None        ⇒ TriggerResult.OtherProblem(s"Unsuccessful response from Jenkins: ${response.status} ${response.statusText}")
      }
      case _ ⇒ TriggerResult.OtherProblem(s"Unsuccessful response from Jenkins: ${response.status} ${response.statusText}")
    }
  }

  private def findNoSuchParamMessage(body: String): Option[String] = {
    val pattern = "No such parameter definition: ([^\r\n]+)".r
    pattern.findFirstMatchIn(body).map(_.group(1))
  }

  private def jenkinsUrl: URI = JenkinsUrlHelper.getServerUrl(jobUrl)

  private def constructPostBody(buildParameters: Seq[BuildParameter], crumbOpt: Option[Crumb]): Map[String, Seq[String]] =
    jsonParam(buildParameters) ++ causeParam ++ crumbParam(crumbOpt) ++ authenticationTokenParam

  private def jsonParam(buildParameters: Seq[BuildParameter]) = Map("json" -> Seq(jsonBuildRequest(buildParameters)))

  /**
   * Construct a JSON build request of the form Jenkins is expecting. Something like:
   *
   * {"parameter": [{"name": "someParameterName", "value": "com.example.Test#testMethod1"}]}
   */
  private def jsonBuildRequest(buildParameters: Seq[BuildParameter]): String = {
    val json = Json.obj("parameter" -> buildParameters.map(_.asJson))
    Json.stringify(json)
  }

  // Cause is only used by Jenkins if you are not logging in as a specific user
  private def causeParam = if (credentialsOpt.isEmpty) Map("cause" -> Seq("Triggered by Test Reporty Thing")) else Map()

  private def authenticationTokenParam = authenticationTokenOpt match {
    case Some(buildToken) ⇒ Map("token" -> Seq(buildToken))
    case None             ⇒ Map()
  }

  private def crumbParam(crumbOpt: Option[Crumb]): Map[String, Seq[String]] =
    crumbOpt.map(_.toParamMap).getOrElse(Map())

}