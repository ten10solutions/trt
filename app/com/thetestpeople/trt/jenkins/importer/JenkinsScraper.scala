package com.thetestpeople.trt.jenkins.importer

import java.net.URI

import scala.xml._

import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http.Credentials
import com.thetestpeople.trt.utils.http._

class JenkinsScraper(
    http: Http,
    credentialsOpt: Option[Credentials],
    fetchConsole: Boolean,
    alreadyImportedBuildUrls: Set[URI]) extends HasLogger {

  type Callback = (JenkinsJob, JenkinsBuild) ⇒ Unit

  def getJenkinsJob(jobUrl: URI): Option[JenkinsJob] =
    for {
      jobXml ← getJobXml(jobUrl)
      jenkinsJob ← parseJobXml(jobUrl, jobXml)
    } yield jenkinsJob

  def getBuildLinks(jobUrl: URI): Seq[JenkinsBuildLink] =
    for {
      jenkinsJob ← getJenkinsJob(jobUrl).toSeq
      buildLink ← jenkinsJob.buildLinks
    } yield buildLink

  def scrapeBuildsFromJob(jobUrl: URI)(buildCallback: Callback): Unit =
    for (jobXml ← getJobXml(jobUrl))
      scrapeBuildsFromJob(jobUrl, jobXml, buildCallback)

  private def scrapeBuildsFromJob(jobUrl: URI, jobXml: Elem, buildCallback: Callback): Unit = {
    for {
      jenkinsJob ← parseJobXml(jobUrl, jobXml).toList
      buildLink ← jenkinsJob.buildLinks
      buildUrl = buildLink.buildUrl
      if !alreadyImportedBuildUrls.contains(buildUrl)
      build ← scrapeBuild(buildUrl, jobUrl)
    } buildCallback(jenkinsJob, build)
  }

  private def parseJobXml(jobUrl: URI, jobXml: Elem): Option[JenkinsJob] =
    try
      Some(new JenkinsJobXmlParser().parse(jobXml))
    catch {
      case e: ParseException ⇒
        logger.error(s"Problem parsing Jenkins job XML from $jobUrl", e)
        None
    }

  private def scrapeTestResult(buildUrl: URI): Option[OrdinaryTestResult] =
    for {
      testXml ← getTestResultsXml(buildUrl)
      testResult ← parseTestResultsXml(testResultsUrl(buildUrl), testXml)
      ordinaryResult ← testResult match {
        case result: MatrixTestResult   ⇒ processMatrixTestResult(result)
        case result: OrdinaryTestResult ⇒ Some(result)
      }
    } yield ordinaryResult

  private def parseTestResultsXml(testUrl: URI, testXml: Elem): Option[TestResult] =
    try
      Some(new JenkinsTestResultXmlParser().parseTestResult(testXml))
    catch {
      case e: ParseException ⇒
        logger.error(s"Problem parsing test result XML from $testUrl", e)
        None
    }

  private def processMatrixTestResult(testResult: MatrixTestResult): Option[OrdinaryTestResult] =
    for {
      childUrl ← testResult.urls.headOption // TODO: handle more than one matrix result
      childTestXml ← getTestResultsXml(childUrl)
      testResult ← parseOrdinaryTestResult(childTestXml)
    } yield testResult

  private def parseOrdinaryTestResult(testResultXml: Elem): Option[OrdinaryTestResult] =
    try
      Some(new JenkinsTestResultXmlParser().parseOrdinaryTestResult(testResultXml))
    catch {
      case e: ParseException ⇒
        logger.error("Problem parsing test result", e)
        None
    }

  def scrapeBuild(buildUrl: URI, jobUrl: URI): Option[JenkinsBuild] = {
    logger.debug(s"scrapeBuild($buildUrl)")
    for {
      buildXml ← getBuildXml(buildUrl)
      buildSummary ← parseBuild(buildUrl, buildXml)
      if buildSummary.hasTestReport
      testResult ← scrapeTestResult(buildUrl)
      consoleTextOpt = if (fetchConsole) getConsole(buildUrl) else None
    } yield JenkinsBuild(jobUrl, buildSummary, testResult, consoleTextOpt)
  }

  private def parseBuild(buildUrl: URI, buildXml: Elem): Option[BuildSummary] =
    try
      Some(new JenkinsBuildXmlParser().parseBuild(buildXml))
    catch {
      case e: ParseException ⇒
        logger.error(s"Problem parsing Jenkins build XML from $buildUrl", e)
        None
    }

  private def getJobXml(jobUrl: URI): Option[Elem] = {
    val url = jobUrl / "api/xml"
    try
      Some(http.get(url, basicAuthOpt = credentialsOpt).bodyAsXml)
    catch {
      case e: HttpException ⇒
        logger.error(s"Problem getting Jenkins job information from $url", e)
        None
    }
  }

  private def getBuildXml(buildUrl: URI): Option[Elem] = {
    val url = buildUrl / "api/xml"
    try
      Some(http.get(url, basicAuthOpt = credentialsOpt).bodyAsXml)
    catch {
      case e: HttpException ⇒
        logger.error(s"Problem getting Jenkins buid information for $url", e)
        None
    }
  }

  private def testResultsUrl(buildUrl: URI): URI = buildUrl / "testReport/api/xml"

  private def getTestResultsXml(buildUrl: URI): Option[Elem] =
    try
      Some(http.get(testResultsUrl(buildUrl), basicAuthOpt = credentialsOpt).bodyAsXml)
    catch {
      case e: HttpException ⇒
        logger.error(s"Problem getting Jenkins test results for build $buildUrl", e)
        None
    }

  private def getConsole(buildUrl: URI): Option[String] =
    try
      Some(http.get(buildUrl / "consoleText", basicAuthOpt = credentialsOpt).body)
    catch {
      case e: HttpException ⇒
        logger.error(s"Problem getting console log for $buildUrl", e)
        None
    }

}