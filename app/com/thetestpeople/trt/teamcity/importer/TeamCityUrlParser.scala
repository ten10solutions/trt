package com.thetestpeople.trt.teamcity.importer

import java.net.URI
import scala.collection.JavaConverters._
import org.apache.http.client.utils.URIBuilder
import org.apache.http.NameValuePair
import java.{ util ⇒ ju }

case class TeamCityConfiguration(serverUrl: URI, buildTypeId: String)

object TeamCityUrlParser {

  def parse(url: URI): Either[String, TeamCityConfiguration] = {
    val builder = new URIBuilder(url)
    val queryParams = getQueryParams(builder)
    getQueryParams(builder).get("buildTypeId") match {
      case None              ⇒ Left("No 'buildTypeId' query parameter found")
      case Some(buildTypeId) ⇒ Right(TeamCityConfiguration(getServerUrl(builder), buildTypeId))
    }
  }

  private def getQueryParams(builder: URIBuilder): Map[String, String] =
    builder.getQueryParams.asScala.map(asPair).toMap

  private def asPair(p: NameValuePair): (String, String) = (p.getName, p.getValue)

  private def getServerUrl(builder: URIBuilder): URI = {
    builder.removeQuery()
    builder.setFragment(null)
    builder.setPath(null)
    builder.build
  }

}