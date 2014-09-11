package com.thetestpeople.trt.jenkins.importer

import java.net.URI

import scala.xml._

import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http.Credentials
import com.thetestpeople.trt.utils.http._

class JenkinsScraperException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

class JenkinsScraper(
    http: Http,
    credentialsOpt: Option[Credentials],
    fetchConsole: Boolean) extends HasLogger {

  @throws[JenkinsScraperException]
  def getJenkinsJob(jobUrl: URI): JenkinsJob = {
    val jobXml = getJobXml(jobUrl)
    parseJobXml(jobUrl, jobXml)
  }

  private def parseJobXml(jobUrl: URI, jobXml: Elem): JenkinsJob =
    try
      new JenkinsJobXmlParser().parse(jobXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsScraperException(s"Problem parsing Jenkins job XML from $jobUrl", e)
    }

  private def scrapeTestResult(buildUrl: URI): Option[OrdinaryTestResult] = {
    val testXml = getTestResultsXml(buildUrl)
    val testResult = parseTestResultsXml(testResultsUrl(buildUrl), testXml)
    testResult match {
      case result: MatrixTestResult   ⇒ processMatrixTestResult(result)
      case result: OrdinaryTestResult ⇒ Some(result)
    }
  }

  private def parseTestResultsXml(testUrl: URI, testXml: Elem): TestResult =
    try
      new JenkinsTestResultXmlParser().parseTestResult(testXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsScraperException(s"Problem parsing test result XML from $testUrl: ${e.getMessage}", e)
    }

  private def processMatrixTestResult(testResult: MatrixTestResult): Option[OrdinaryTestResult] =
    for {
      childUrl ← testResult.urls.headOption // TODO: handle more than one matrix result
      childTestXml = getTestResultsXml(childUrl)
    } yield parseOrdinaryTestResult(childTestXml)

  private def parseOrdinaryTestResult(testResultXml: Elem): OrdinaryTestResult =
    try
      new JenkinsTestResultXmlParser().parseOrdinaryTestResult(testResultXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsScraperException(s"Problem parsing test result: ${e.getMessage}", e)
    }

  @throws[JenkinsScraperException]
  def scrapeBuild(buildUrl: URI, jobUrl: URI): Option[JenkinsBuild] = {
    logger.debug(s"scrapeBuild($buildUrl)")
    val buildXml = getBuildXml(buildUrl)
    val buildSummary = parseBuild(buildUrl, buildXml)
    if (buildSummary.hasTestReport) {
      scrapeTestResult(buildUrl)
      for {
        testResult ← scrapeTestResult(buildUrl)
        consoleTextOpt = if (fetchConsole) Some(getConsole(buildUrl)) else None
      } yield JenkinsBuild(jobUrl, buildSummary, testResult, consoleTextOpt)
    } else
      None
  }

  private def parseBuild(buildUrl: URI, buildXml: Elem): BuildSummary =
    try
      new JenkinsBuildXmlParser().parseBuild(buildXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsScraperException(s"Problem parsing Jenkins build XML from $buildUrl", e)
    }

  private def getJobXml(jobUrl: URI): Elem = {
    val url = jobUrl / "api/xml"
    try
      http.get(url, basicAuthOpt = credentialsOpt).bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new JenkinsScraperException(s"Problem getting Jenkins job information from $url", e)
    }
  }

  private def getBuildXml(buildUrl: URI): Elem = {
    val url = buildUrl / "api/xml"
    try
      http.get(url, basicAuthOpt = credentialsOpt).bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new JenkinsScraperException(s"Problem getting Jenkins build information from $url", e)
    }
  }

  private def testResultsUrl(buildUrl: URI): URI = buildUrl / "testReport/api/xml"

  private def getTestResultsXml(buildUrl: URI): Elem =
    try
      http.get(testResultsUrl(buildUrl), basicAuthOpt = credentialsOpt).bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new JenkinsScraperException(s"Problem getting Jenkins test results for build $buildUrl", e)
    }

  private def getConsole(buildUrl: URI): String =
    try
      http.get(buildUrl / "consoleText", basicAuthOpt = credentialsOpt).body
    catch {
      case e: HttpException ⇒
        throw new JenkinsScraperException(s"Problem getting console log for $buildUrl", e)
    }

}