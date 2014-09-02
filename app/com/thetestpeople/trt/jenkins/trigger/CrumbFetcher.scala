package com.thetestpeople.trt.jenkins.trigger

import java.net.URI

import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http.Credentials
import com.thetestpeople.trt.utils.http.Http

sealed trait CrumbFetchResult

object CrumbFetchResult {

  case class AuthenticationProblem(message: String) extends CrumbFetchResult
  case object NotFound extends CrumbFetchResult
  case class Found(crumb: Crumb) extends CrumbFetchResult
  case class Other(message: String) extends CrumbFetchResult

}

class CrumbFetcher(http: Http, jenkinsUrl: URI, credentialsOpt: Option[Credentials] = None) extends HasLogger {

  private val crumbUrl = jenkinsUrl / "crumbIssuer/api/json"

  /**
   * Fetch crumb (CSRF token), if available
   */
  def getCrumb(): CrumbFetchResult =
    try {
      val response = http.get(crumbUrl, basicAuthOpt = credentialsOpt)
      response.status match {
        case 200       ⇒ CrumbFetchResult.Found(Crumb.fromJson(response.bodyAsJson))
        case 401 | 403 ⇒ CrumbFetchResult.AuthenticationProblem(s"${response.status} ${response.statusText}")
        case 404       ⇒ CrumbFetchResult.NotFound
        case _         ⇒ CrumbFetchResult.Other(s"Problem fetching crumb from $crumbUrl: ${response.status} ${response.statusText}")
      }
    } catch {
      case e: Exception ⇒
        logger.error("Problem fetching crumb from $crumbUrl", e)
        CrumbFetchResult.Other(s"Problem fetching crumb from $crumbUrl: ${e.getMessage}")
    }

}