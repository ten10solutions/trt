package com.thetestpeople.trt.jenkins.importer

import org.joda.time.Duration
import java.net.URI

sealed trait TestResult
case class MatrixTestResult(urls: List[URI]) extends TestResult
case class OrdinaryTestResult(duration: Duration, suites: List[Suite]) extends TestResult

case class Suite(name: String, duration: Duration, cases: List[Case])

case class Case(
    name: String,
    className: String,
    duration: Duration,
    status: String,
    stdoutOpt: Option[String],
    errorDetailsOpt: Option[String],
    errorStackTraceOpt: Option[String]) {

  def passed: Boolean = status match {
    case "PASSED"     ⇒ true
    case "FIXED"      ⇒ true
    case "FAILED"     ⇒ false
    case "REGRESSION" ⇒ false
    case "SKIPPED"    ⇒ true
    case _            ⇒ throw new ParseException(s"Unknown status $status")
  }

  def skipped: Boolean = status == "SKIPPED"

}
