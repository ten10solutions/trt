package com.thetestpeople.trt.importer.teamcity

import org.joda.time.DateTime
import com.thetestpeople.trt.service.Incoming
import com.thetestpeople.trt.model.Configuration
import com.github.nscala_time.time.Imports._
import scala.PartialFunction.condOpt

class TeamCityBatchCreator(configurationOpt: Option[Configuration]) {

  def createBatch(build: TeamCityBuild): Incoming.Batch =
    Incoming.Batch(
      complete = true,
      executions = build.occurrences map createExecution,
      urlOpt = Some(build.url),
      nameOpt = Some(build.number),
      logOpt = None,
      executionTimeOpt = Some(build.startDate),
      durationOpt = Some(new Duration(build.startDate, build.finishDate)),
      configurationOpt = configurationOpt)

  private def createExecution(occurrence: TeamCityTestOccurrence): Incoming.Execution =
    Incoming.Execution(
      test = createTest(occurrence.testName),
      passed = occurrence.passed,
      summaryOpt = None,
      logOpt = occurrence.detailOpt,
      executionTimeOpt = None,
      durationOpt = occurrence.durationOpt,
      configurationOpt = configurationOpt)

  private def stripSuitePrefix(s: String) = s.drop(s.indexOf(":") + 1)

  private object QualifiedTestName {

    def unapply(s: String): Option[(String, String)] = condOpt(s lastIndexOf ".") {
      case i if i >= 0 ⇒ (s.take(i), s.drop(i + 1))
    }

  }

  private def createTest(testName: String): Incoming.Test =
    stripSuitePrefix(testName) match {
      case QualifiedTestName(group, name) ⇒
        Incoming.Test(name = name, groupOpt = Some(group))
      case name ⇒
        Incoming.Test(name = name)
    }

}