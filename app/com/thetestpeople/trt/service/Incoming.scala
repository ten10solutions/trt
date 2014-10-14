package com.thetestpeople.trt.service

import com.thetestpeople.trt.model._
import org.joda.time._
import java.net.URI

/**
 * Types representing incoming test execution records
 */
object Incoming {

  trait AbtractExecution {

    def logOpt: Option[String]

    def executionTimeOpt: Option[DateTime]

    def durationOpt: Option[Duration]

  }

  case class Batch(
    executions: Seq[Execution],
    urlOpt: Option[URI],
    nameOpt: Option[String],
    logOpt: Option[String],
    executionTimeOpt: Option[DateTime],
    durationOpt: Option[Duration],
    configurationOpt: Option[Configuration]) extends AbtractExecution

  case class Execution(
      test: Test,
      passed: Boolean,
      summaryOpt: Option[String],
      logOpt: Option[String],
      executionTimeOpt: Option[DateTime],
      durationOpt: Option[Duration],
      configurationOpt: Option[Configuration]) extends AbtractExecution {

    def failed = !passed

  }

  case class Test(name: String, groupOpt: Option[String] = None) {
    def qualifiedName = QualifiedName(name, groupOpt)
  }

}
