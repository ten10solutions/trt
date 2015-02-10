package com.thetestpeople.trt.importer.jenkins

import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.service.Incoming
import com.thetestpeople.trt.model.Configuration
import java.net.URI

/**
 * Translate information from a Jenkins build into the test result import data structure.
 */
class JenkinsBatchCreator(configurationOpt: Option[Configuration]) {

  def createBatch(build: JenkinsBuild): Incoming.Batch = {
    val buildSummary = build.buildSummary
    val executions =
      for {
        testResult ← build.testResults
        configurationOpt = testResult.matrixConfigurationOpt.map(calculateConfiguration).orElse(this.configurationOpt)
        suite ← testResult.suites
        testCase ← suite.cases
        if !testCase.skipped
      } yield createExecution(buildSummary.timestampOpt, testCase, configurationOpt)
    val duration = build.testResults.map(_.duration).max
    Incoming.Batch(
      complete = true,
      executions = executions,
      urlOpt = Some(build.buildSummary.url),
      nameOpt = build.buildSummary.nameOpt,
      logOpt = build.consoleTextOpt,
      executionTimeOpt = buildSummary.timestampOpt,
      durationOpt = Some(duration),
      configurationOpt = configurationOpt)
  }

  private def calculateConfiguration(matrixConfiguration: MatrixConfiguration): Configuration =
    Configuration(matrixConfiguration.axisValues.map(_.value).mkString("/"))

  private def createLog(testCase: Case): Option[String] = {
    val sb = new StringBuilder
    for (errorStackTrace ← testCase.errorStackTraceOpt if errorStackTrace.nonEmpty)
      sb.append("Stack trace:\n" + errorStackTrace)
    for (stdout ← testCase.stdoutOpt if stdout.nonEmpty)
      sb.append("Standard out:\n" + stdout)
    sb.toString match {
      case "" ⇒ None
      case s  ⇒ Some(s)
    }
  }

  private def createTest(testCase: Case): Incoming.Test =
    Incoming.Test(
      name = testCase.name,
      groupOpt = Some(testCase.className),
      categories = Seq())

  private def createExecution(buildTimeStampOpt: Option[DateTime], testCase: Case, configurationOpt: Option[Configuration]): Incoming.Execution =
    Incoming.Execution(
      test = createTest(testCase),
      passed = testCase.passed,
      summaryOpt = testCase.errorDetailsOpt,
      logOpt = createLog(testCase),
      executionTimeOpt = buildTimeStampOpt,
      durationOpt = Some(testCase.duration),
      configurationOpt = configurationOpt)

}