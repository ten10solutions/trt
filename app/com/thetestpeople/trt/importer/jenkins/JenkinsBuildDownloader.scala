package com.thetestpeople.trt.importer.jenkins

import java.net.URI
import scala.xml._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http.Credentials
import com.thetestpeople.trt.utils.http._
import com.thetestpeople.trt.utils.UriUtils._

class JenkinsBuildDownloaderException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

class JenkinsBuildDownloader(
    http: Http,
    credentialsOpt: Option[Credentials],
    fetchConsole: Boolean) extends HasLogger {

  @throws[JenkinsBuildDownloaderException]
  def getJenkinsJob(jobUrl: URI): JenkinsJob = {
    val jobXml = getJobXml(jobUrl)
    var job = parseJobXml(jobUrl, jobXml)
    job = job.copy(url = job.url.withSameHostAndPortAs(jobUrl))
    job = job.copy(buildLinks = job.buildLinks.map(bl ⇒ bl.copy(buildUrl = bl.buildUrl.withSameHostAndPortAs(jobUrl))))
    job
  }

  private def getJobXml(jobUrl: URI): Elem = {
    // For jenkins, we need to explicitly set the tree param to fetch the otherwise-hidden "allBuilds" list. Ordinarily the builds
    // are capped at 100. See https://issues.jenkins-ci.org/browse/JENKINS-22977
    // Hudson doesn't support "allBuilds", so we include "builds" as well (and we later remove duplicates)
    val url = (jobUrl / "api/xml") ? "tree=name,url,allBuilds[number,url],builds[number,url]"
    try
      http.get(url, basicAuthOpt = credentialsOpt).checkOK.bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem getting Jenkins job information from $url: ${e.getMessage}", e)
    }
  }

  private def parseJobXml(jobUrl: URI, jobXml: Elem): JenkinsJob =
    try
      new JenkinsJobXmlParser().parse(jobXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem parsing Jenkins job XML from $jobUrl", e)
    }

  private def scrapeTestResults(buildUrl: URI): Seq[OrdinaryTestResult] = {
    val testXml = getTestResultsXml(buildUrl)
    val testResult = parseTestResultsXml(testResultsUrl(buildUrl), testXml)
    testResult match {
      case result: OrdinaryTestResult   ⇒ Seq(result)
      case result: MatrixTestResult     ⇒ processMatrixTestResult(result, buildUrl)
      case result: AggregatedTestResult ⇒ processAggregatedTestResult(result, buildUrl)
    }
  }

  private def parseTestResultsXml(testUrl: URI, testXml: Elem): TestResult =
    try
      new JenkinsTestResultXmlParser().parseTestResult(testXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem parsing test result XML from $testUrl: ${e.getMessage}", e)
    }

  private def processAggregatedTestResult(testResult: AggregatedTestResult, buildUrl: URI): Seq[OrdinaryTestResult] =
    for {
      childUrl ← testResult.childUrls
      actualChildUrl = childUrl.withSameHostAndPortAs(buildUrl)
      childTestXml = getTestResultsXml(actualChildUrl)
    } yield parseOrdinaryTestResult(childTestXml)

  private def processMatrixTestResult(testResult: MatrixTestResult, buildUrl: URI): Seq[OrdinaryTestResult] =
    for {
      childUrl ← testResult.childUrls
      actualChildUrl = childUrl.withSameHostAndPortAs(buildUrl)
      childTestXml = getTestResultsXml(actualChildUrl)
      matrixConfigurationOpt = MatrixJobUrlParser.getConfigurations(childUrl)
    } yield parseOrdinaryTestResult(childTestXml).copy(matrixConfigurationOpt = matrixConfigurationOpt)

  private def parseOrdinaryTestResult(testResultXml: Elem): OrdinaryTestResult =
    try
      new JenkinsTestResultXmlParser().parseOrdinaryTestResult(testResultXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem parsing test result: ${e.getMessage}", e)
    }

  /**
   * @return None if build has no tests associated with it, or if it is still building
   */
  @throws[JenkinsBuildDownloaderException]
  def getJenkinsBuild(buildUrl: URI, jobUrl: URI): Option[JenkinsBuild] = {
    val buildXml = getBuildXml(buildUrl)
    val buildSummary = parseBuild(buildUrl, buildXml)

    // Note: it's not sufficient to check buildSummary.hasTestReport, as you can have a partial test report while building, 
    // e.g. a multimodule Maven project.
    if (buildSummary.hasTestReport && !buildSummary.isBuilding) {
      val testResults = scrapeTestResults(buildUrl)
      if (testResults.isEmpty)
        None
      else {
        val consoleTextOpt = if (fetchConsole) Some(getConsole(buildUrl)) else None
        Some(JenkinsBuild(jobUrl, buildSummary, testResults, consoleTextOpt))
      }
    } else
      None
  }

  private def parseBuild(buildUrl: URI, buildXml: Elem): JenkinsBuildSummary =
    try
      new JenkinsBuildXmlParser().parseBuild(buildXml)
    catch {
      case e: ParseException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem parsing Jenkins build XML from $buildUrl: ${e.getMessage}", e)
    }

  private def getBuildXml(buildUrl: URI): Elem = {
    val url = buildUrl / "api/xml"
    try
      http.get(url, basicAuthOpt = credentialsOpt).checkOK.bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem getting Jenkins build information from $url: ${e.getMessage}", e)
    }
  }

  private def testResultsUrl(buildUrl: URI): URI = buildUrl / "testReport/api/xml"

  private def getTestResultsXml(buildUrl: URI): Elem =
    try
      http.get(testResultsUrl(buildUrl), basicAuthOpt = credentialsOpt).checkOK.bodyAsXml
    catch {
      case e: HttpException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem getting Jenkins test results for build $buildUrl: ${e.getMessage}", e)
    }

  private def getConsole(buildUrl: URI): String =
    try
      http.get(buildUrl / "consoleText", basicAuthOpt = credentialsOpt).checkOK.body
    catch {
      case e: HttpException ⇒
        throw new JenkinsBuildDownloaderException(s"Problem getting console log for $buildUrl: ${e.getMessage}", e)
    }

}