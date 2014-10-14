package com.thetestpeople.trt.jenkins.importer

import org.joda.time.Duration
import scala.xml._
import scala.BigDecimal.RoundingMode
import java.net.URI

class JenkinsTestResultXmlParser {

  @throws[ParseException]
  def parseTestResult(xml: String): TestResult = {
    val rootElem =
      try
        XML.loadString(xml)
      catch {
        case e: Throwable ⇒
          throw new ParseException("Problem parsing Jenkins test XML", e)
      }
    parseTestResult(rootElem)
  }

  @throws[ParseException]
  def parseTestResult(root: Elem): TestResult = {
    if (root.label == "matrixTestResult" )
      MatrixTestResult(getChildReportUrls(root))
    else if (root.label == "surefireAggregatedReport")
      AggregatedTestResult(getChildReportUrls(root))
    else if (root.label == "testResult")
      parseOrdinaryTestResult(root)
    else
      throw new ParseException(s"Unknown root element <${root.label}>")
  }

  private def getChildReportUrls(root: Elem): Seq[URI] =
    (root \ "childReport" \ "child" \ "url").map(_.text).map(new URI(_))

  def parseOrdinaryTestResult(root: Elem): OrdinaryTestResult = {
    val durationString = getFieldOpt(root, "duration").getOrElse(
      throw new ParseException(s"No <duration> element within <${root.label}>"))
    val duration = asDuration(parseDecimal(durationString))
    val suites = (root \ "suite").map(parseSuite)
    OrdinaryTestResult(duration = duration, suites = suites)
  }

  private def parseSuite(node: Node): Suite = {
    val durationString = getFieldOpt(node, "duration").getOrElse(
      throw new ParseException("No <duration> element within <suite>"))
    val duration = asDuration(parseDecimal(durationString))
    val cases = (node \ "case").map(parseCase)
    val name = getFieldOpt(node, "name").getOrElse(
      throw new ParseException("No <name> element within <suite>"))
    Suite(name = name, duration = duration, cases = cases)
  }

  private def parseCase(node: Node): Case = {
    val durationString = getFieldOpt(node, "duration").getOrElse(
      throw new ParseException("No <duration> element within <case>"))
    val duration = asDuration(parseDecimal(durationString))
    val name = getFieldOpt(node, "name").getOrElse(
      throw new ParseException("No <name> element within <case>"))
    val className = getFieldOpt(node, "className").getOrElse(
      throw new ParseException("No <className> element within <case>"))
    val status = getFieldOpt(node, "status").getOrElse(
      throw new ParseException("No <status> element within <case>"))
    val stdoutOpt = getFieldOpt(node, "stdout")
    val errorDetailsOpt = getFieldOpt(node, "errorDetails")
    val errorStackTraceOpt = getFieldOpt(node, "errorStackTrace")
    Case(
      name = name,
      className = className,
      duration = duration,
      status = status,
      stdoutOpt = stdoutOpt,
      errorDetailsOpt = errorDetailsOpt,
      errorStackTraceOpt = errorStackTraceOpt)
  }

  private def getFieldOpt(node: Node, name: String): Option[String] =
    (node \ (name)).headOption.map(_.text)

  private def parseDecimal(s: String): BigDecimal =
    try BigDecimal(s)
    catch {
      case e: NumberFormatException ⇒
        throw ParseException(s"Cannot parse '$s' as a decimal", e)
    }

  private def asDuration(seconds: BigDecimal): Duration =
    Duration.millis((seconds.setScale(3, RoundingMode.HALF_UP) * 1000).toInt)

}

case class ParseException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)