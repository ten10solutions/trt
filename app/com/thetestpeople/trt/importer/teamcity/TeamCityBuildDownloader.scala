package com.thetestpeople.trt.importer.teamcity

import java.net.URI

import scala.collection.mutable.ListBuffer
import scala.xml._

import org.apache.http.client.utils.URIBuilder

import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.http._

class TeamCityDownloadException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

/**
 * Downloads information about TeamCity builds using its REST API
 */
class TeamCityBuildDownloader(http: Http, jobLink: TeamCityJobLink, credentialsOpt: Option[Credentials] = None) extends HasLogger {

  private val authRoute = if (credentialsOpt.isDefined) "httpAuth" else "guestAuth"

  private val parser = new TeamCityXmlParser

  /**
   * Get information about a build type (configuration), and list of builds that are available to download
   */
  @throws[TeamCityDownloadException]
  def getBuildType(): (TeamCityBuildType, Seq[TeamCityBuildLink]) = {
    val url = buildTypeUrl
    val xml = fetchXml(url)
    val buildType = parseBuildTypeXml(url, xml)
    val buildLinks =
      buildType.buildsPathOpt.toSeq.flatMap { buildsPath ⇒
        val url = jobLink.relativePath(buildsPath)
        val xml = fetchXml(url)
        parseBuildListXml(url, xml)
      }
    (buildType, buildLinks)
  }

  /**
   * Download all the test results for the given build
   */
  @throws[TeamCityDownloadException]
  def getBuild(buildLink: TeamCityBuildLink): TeamCityBuild = {
    val url = jobLink.relativePath(buildLink.buildPath)
    val xml = fetchXml(url)
    var build = parseBuildXml(url, xml)
    for (testOccurrencesPath ← build.testOccurrencesPathOpt) {
      val occurrences = fetchAllTestOccurrences(testOccurrencesPath)
      build = build.copy(occurrences = occurrences)
    }
    build
  }

  private def buildTypeUrl: URI = {
    val builder = new URIBuilder(jobLink.serverUrl)
    builder.setPath(s"/$authRoute/app/rest/buildTypes/id:${jobLink.buildTypeId}")
    builder.build
  }

  private def fetchXml(url: URI): Elem =
    try
      http.get(url, basicAuthOpt = credentialsOpt).checkOK.bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new TeamCityDownloadException(s"Problem getting XML from $url: ${e.getMessage}", e)
    }

  private def parseBuildListXml(url: URI, xml: Elem) =
    try
      parser.parseBuildLinks(xml)
    catch {
      case e: TeamCityXmlParseException ⇒
        throw new TeamCityDownloadException(s"Problem parsing TeamCity build list XML from $url", e)
    }

  private def parseBuildTypeXml(url: URI, xml: Elem) =
    try
      parser.parseBuildType(xml)
    catch {
      case e: TeamCityXmlParseException ⇒
        throw new TeamCityDownloadException(s"Problem parsing TeamCity build type XML from $url", e)
    }

  private def parseBuildXml(url: URI, xml: Elem) =
    try
      parser.parseBuild(xml)
    catch {
      case e: TeamCityXmlParseException ⇒
        throw new TeamCityDownloadException(s"Problem parsing TeamCity build XML from $url", e)
    }

  private def parseTestOccurrenceXml(url: URI, xml: Elem) =
    try
      parser.parseTestOccurrence(xml)
    catch {
      case e: TeamCityXmlParseException ⇒
        throw new TeamCityDownloadException(s"Problem parsing TeamCity test occurrence XML from $url", e)
    }

  private def parseTestOccurrencesXml(url: URI, xml: Elem) =
    try
      parser.parseTestOccurrences(xml)
    catch {
      case e: TeamCityXmlParseException ⇒
        throw new TeamCityDownloadException(s"Problem parsing TeamCity test occurrences XML from $url", e)
    }

  /**
   * Fetch all test occurrences from the given path -- and if the results are paged, recursively fetch
   *   the subsequent pages too.
   */
  private def fetchAllTestOccurrences(testOccurrencesPath: String): Seq[TeamCityTestOccurrence] = {
    val allOccurrencePaths = ListBuffer[String]()
    fetchAllTestOccurrences(testOccurrencesPath, allOccurrencePaths)
    allOccurrencePaths.map(getTestOccurrence)
  }

  private def fetchAllTestOccurrences(testOccurrencesPath: String, allOccurrencePaths: ListBuffer[String]) {
    val TeamCityTestOccurrences(nextLinkOpt, occurrencePaths) = getTestOccurrencePaths(testOccurrencesPath)
    allOccurrencePaths ++= occurrencePaths
    for (nextLink ← nextLinkOpt)
      fetchAllTestOccurrences(nextLink, allOccurrencePaths)
  }

  private def getTestOccurrence(occurrencePath: String): TeamCityTestOccurrence = {
    val url = jobLink.relativePath(occurrencePath)
    val xml = fetchXml(url)
    parseTestOccurrenceXml(url, xml)
  }

  private def getTestOccurrencePaths(occurrencesPath: String): TeamCityTestOccurrences = {
    val url = jobLink.relativePath(occurrencesPath)
    val xml = fetchXml(url)
    parseTestOccurrencesXml(url, xml)
  }

}