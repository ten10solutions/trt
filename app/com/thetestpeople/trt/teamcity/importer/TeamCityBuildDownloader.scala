package com.thetestpeople.trt.teamcity.importer

import com.thetestpeople.trt.utils.http._
import java.net.URI
import org.apache.http.client.utils.URIBuilder
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import scala.collection.mutable.ListBuffer
import scala.annotation.tailrec

/**
 * Downloads information about TeamCity builds using its REST API
 */
class TeamCityBuildDownloader(http: Http, configuration: TeamCityConfiguration) extends HasLogger {

  private val authRoute = "guestAuth"

  private def buildsUrl: URI = {
    val builder = new URIBuilder(configuration.serverUrl)
    builder.setPath(s"/$authRoute/app/rest/builds/")
    builder.addParameter("locator", s"buildType:${configuration.buildTypeId}")
    builder.build
  }

  def getBuildLinks(): Seq[TeamCityBuildLink] = {
    println("getBuildLinks()")
    val response = http.get(buildsUrl)
    val buildsXml = response.bodyAsXml
    val parser = new TeamCityXmlParser
    parser.parseBuildLinks(buildsXml)
  }

  private def getAllTestOccurrences(testOccurrencesPath: String, allOccurrencePaths: ListBuffer[String]) {
    val TeamCityTestOccurrences(nextLinkOpt, occurrencePaths) = getTestOccurrencePaths(testOccurrencesPath)
    allOccurrencePaths ++= occurrencePaths
    for (nextLink ← nextLinkOpt)
      getAllTestOccurrences(nextLink, allOccurrencePaths)
  }

  def getBuild(buildLink: TeamCityBuildLink): TeamCityBuild = {
    println(s"getBuild($buildLink)")
    val buildUrl = concat(configuration.serverUrl, buildLink.buildPath)
    val response = http.get(buildUrl)
    val buildXml = response.bodyAsXml
    val parser = new TeamCityXmlParser
    var build = parser.parseBuild(buildXml)
    for (testOccurrencesPath ← build.testOccurrencesPathOpt) {
      val allOccurrencePaths = ListBuffer[String]()
      getAllTestOccurrences(testOccurrencesPath, allOccurrencePaths)
      val occurrences = allOccurrencePaths.toSeq.map(getTestOccurrence)
      build = build.copy(occurrences = occurrences)
    }
    build
  }

  private def getTestOccurrence(occurrencePath: String): TeamCityTestOccurrence = {
    println(s"getTestOccurrence($occurrencePath)")
    val parser = new TeamCityXmlParser
    val occurrenceUrl = concat(configuration.serverUrl, occurrencePath)
    val response = http.get(occurrenceUrl)
    val occurrenceXml = response.bodyAsXml
    parser.parseTestOccurrence(occurrenceXml)
  }

  private def getTestOccurrencePaths(occurrencesPath: String): TeamCityTestOccurrences = {
    println(s"getTestOccurrencePaths($occurrencesPath)")
    val parser = new TeamCityXmlParser
    val occurrencesUrl = concat(configuration.serverUrl, occurrencesPath)
    val response = http.get(occurrencesUrl)
    val occurrencesXml = response.bodyAsXml
    parser.parseTestOccurrences(occurrencesXml)
  }

  private def concat(url: URI, pathAndQuery: String): URI = uri(url.toString + pathAndQuery)

}