package com.thetestpeople.trt.importer.jenkins

import org.joda.time.DateTime
import com.thetestpeople.trt.service.Incoming
import com.thetestpeople.trt.model.Configuration

/**
 * Translate information from a Jenkins build into the test result import data structure.
 */
class JenkinsBatchCreator(configurationOpt: Option[Configuration]) {

  def createBatch(build: JenkinsBuild): Incoming.Batch = {
    val testResult = build.testResult
    val buildSummary = build.buildSummary
    val executions =
      for {
        suite ← testResult.suites
        testCase ← suite.cases
        if !testCase.skipped
      } yield createExecution(buildSummary.timestampOpt)(testCase)
    Incoming.Batch(
      complete = true,
      executions = executions,
      urlOpt = Some(build.buildSummary.url),
      nameOpt = build.buildSummary.nameOpt,
      logOpt = build.consoleTextOpt,
      executionTimeOpt = buildSummary.timestampOpt,
      durationOpt = Some(testResult.duration),
      configurationOpt = configurationOpt)
  }

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

  private def createExecution(buildTimeStampOpt: Option[DateTime])(testCase: Case): Incoming.Execution =
    Incoming.Execution(
      test = createTest(testCase),
      passed = testCase.passed,
      summaryOpt = testCase.errorDetailsOpt,
      logOpt = createLog(testCase),
      executionTimeOpt = buildTimeStampOpt,
      durationOpt = Some(testCase.duration),
      configurationOpt = configurationOpt)

}