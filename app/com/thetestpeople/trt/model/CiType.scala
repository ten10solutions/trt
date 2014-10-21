package com.thetestpeople.trt.model

import java.net.URI
import org.apache.http.client.utils.URIBuilder
import com.thetestpeople.trt.importer.teamcity.TeamCityUrlParser

object CiType {

  val Jenkins = CiType("Jenkins")
  val TeamCity = CiType("TeamCity")

  /**
   * Infer type of CI server from job URL.
   */
  def inferCiType(url: URI): Option[CiType] = {
    val builder = new URIBuilder(url)
    if (url.getPath.contains("/job/"))
      Some(CiType.Jenkins)
    else if (TeamCityUrlParser.parse(url).isRight)
      Some(CiType.TeamCity)
    else
      None
  }

}

case class CiType(name: String) {

}