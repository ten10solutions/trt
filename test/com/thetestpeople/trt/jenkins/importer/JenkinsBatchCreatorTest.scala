package com.thetestpeople.trt.jenkins.importer

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model.impl.DummyData
import com.github.nscala_time.time.Imports._
import java.net.URI

@RunWith(classOf[JUnitRunner])
class JenkinsBatchCreatorTest extends FlatSpec with Matchers {

  "Creating an incoming batch record from a Jenkins build" should "capture all the relevant data" in {
    val buildSummary = makeBuildSummary()
    val case1: Case = makeCase()
    val suite = makeSuite(cases = List(case1))
    val testResult = makeTestResult(suites = List(suite))
    val consoleLog = DummyData.Log
    val jenkinsBuild = JenkinsBuild(DummyData.JobUrl, buildSummary, testResult, Some(consoleLog))

    val batch = new JenkinsBatchCreator(configurationOpt = None).createBatch(jenkinsBuild)

    batch.nameOpt should equal(buildSummary.nameOpt)
    batch.executionTimeOpt should equal(buildSummary.timestampOpt)
    batch.durationOpt should equal(buildSummary.durationOpt)
    batch.logOpt should equal(Some(consoleLog))
    batch.urlOpt should equal(Some(buildSummary.url))

    val List(execution) = batch.executions
    execution.durationOpt should equal(Some(case1.duration))
    execution.executionTimeOpt should equal(buildSummary.timestampOpt)
    execution.passed should equal(case1.passed)
    execution.summaryOpt should be(None)
    execution.logOpt should equal(Some("Standard out:\n" + case1.stdoutOpt.get))

    val test = execution.test
    test.name should equal(case1.name)
    test.groupOpt should equal(Some(case1.className))
  }

  private def makeTestResult(duration: Duration = DummyData.Duration, suites: List[Suite]): OrdinaryTestResult =
    OrdinaryTestResult(duration = duration, suites = suites)

  private def makeBuildSummary(
    url: URI = DummyData.BuildUrl,
    durationOpt: Option[Duration] = Some(DummyData.Duration),
    nameOpt: Option[String] = Some(DummyData.BatchName),
    timestampOpt: Option[DateTime] = Some(DummyData.ExecutionTime),
    resultOpt: Option[String] = Some("PASSED"),
    hasTestReport: Boolean = true,
    isBuilding: Boolean = false) =
    JenkinsBuildSummary(
      url = url,
      durationOpt = durationOpt,
      nameOpt = nameOpt,
      timestampOpt = timestampOpt,
      resultOpt = resultOpt,
      hasTestReport = hasTestReport,
      isBuilding = isBuilding)

  private def makeSuite(
    name: String = DummyData.Group,
    duration: Duration = DummyData.Duration,
    cases: List[Case]) =
    Suite(
      name = name,
      duration = duration,
      cases = cases)

  private def makeCase(
    name: String = DummyData.TestName,
    className: String = DummyData.Group,
    duration: Duration = DummyData.Duration,
    status: String = "PASSED",
    stdoutOpt: Option[String] = Some("stdout"),
    errorDetailsOpt: Option[String] = None,
    errorStackTraceOpt: Option[String] = None): Case =
    Case(
      name = name,
      className = className,
      duration = duration,
      status = status,
      stdoutOpt = stdoutOpt,
      errorDetailsOpt = errorDetailsOpt,
      errorStackTraceOpt = errorStackTraceOpt)

}