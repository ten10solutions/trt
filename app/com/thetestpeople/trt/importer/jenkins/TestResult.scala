package com.thetestpeople.trt.importer.jenkins

import org.joda.time.Duration

import java.net.URI
import com.github.nscala_time.time.Imports._
sealed trait TestResult {
  def childUrls: Seq[URI]
}

case class AggregatedTestResult(childUrls: Seq[URI]) extends TestResult
case class MatrixTestResult(childUrls: Seq[URI]) extends TestResult

case class OrdinaryTestResult(
    duration: Duration,
    suites: Seq[Suite],
    matrixConfigurationOpt: Option[MatrixConfiguration] = None) extends TestResult {
  def childUrls: Seq[URI] = Seq()
}

case class Suite(name: String, duration: Duration, cases: Seq[Case])

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
