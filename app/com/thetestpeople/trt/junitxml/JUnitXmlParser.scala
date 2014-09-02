package com.thetestpeople.trt.junitxml

import scala.BigDecimal.RoundingMode
import scala.xml._

import org.joda.time._
import org.joda.time.format.ISODateTimeFormat

case class ParseException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

class JUnitXmlParser {

  @throws[ParseException]
  def parseTestSuite(xml: String): TestSuite = {
    val rootElem =
      try
        XML.loadString(xml)
      catch {
        case e: Throwable ⇒
          throw new ParseException("Problem parsing JUnit XML", e)
      }
    parseTestSuite(rootElem)
  }

  @throws[ParseException]
  def parseTestSuite(suiteNode: Elem): TestSuite = {
    val label = suiteNode.label
    if (suiteNode.label != "testsuite")
      throw ParseException(s"Could not parse, root element must be 'testSuite', but was '$label'")

    val errorsOpt = getAttribute(suiteNode, "errors").map(parseInt)
    val failuresOpt = getAttribute(suiteNode, "failures").map(parseInt)
    val testsOpt = getAttribute(suiteNode, "tests").map(parseInt)
    val hostnameOpt = getAttribute(suiteNode, "hostname")
    val timeOpt = getAttribute(suiteNode, "time").map(parseDecimal).map(asDuration)
    val name = getAttribute(suiteNode, "name").getOrElse(
      throw new ParseException("No 'name' field supplied on <testsuite/>"))
    val timestampOpt = getAttribute(suiteNode, "timestamp").map(parseDateTime)
    val sysoutOpt = (suiteNode \ "system-out").headOption.map(_.text)
    val syserrOpt = (suiteNode \ "system-err").headOption.map(_.text)
    val properties = getProperties(suiteNode)
    val testCases = (suiteNode \ "testcase").toList.map(parseCase)
    TestSuite(
      name = name,
      testCases = testCases,
      errorsOpt = errorsOpt,
      failuresOpt = failuresOpt,
      testsOpt = testsOpt,
      timeOpt = timeOpt,
      hostnameOpt = hostnameOpt,
      timestampOpt = None,
      properties = properties,
      sysoutOpt = sysoutOpt,
      syserrOpt = syserrOpt)
  }

  private def getAttribute(node: Node, name: String): Option[String] =
    (node \ ("@" + name)).headOption.map(_.text)

  private def parseInt(s: String): Int =
    try
      s.toInt
    catch {
      case e: NumberFormatException ⇒
        throw ParseException(s"Cannot parse '$s' as an integer", e)
    }

  private def parseDecimal(s: String): BigDecimal =
    try
      BigDecimal(s)
    catch {
      case e: NumberFormatException ⇒
        throw ParseException(s"Cannot parse '$s' as a decimal", e)
    }

  private def asDuration(seconds: BigDecimal): Duration =
    Duration.millis((seconds.setScale(3, RoundingMode.HALF_UP) * 1000).toInt)

  private def parseDateTime(s: String): DateTime =
    try
      ISODateTimeFormat.dateTimeParser().parseDateTime(s)
    catch {
      case e: IllegalArgumentException ⇒
        throw ParseException(s"Cannot parse '$s' as a timestamp", e)
    }

  private def getProperties(suiteNode: Elem): Map[String, String] = {
    val propertyPairs =
      for {
        propertiesNode ← suiteNode \ "properties"
        propertyNode ← propertiesNode \ "property"
        name ← getAttribute(propertyNode, "name")
        value ← getAttribute(propertyNode, "value")
      } yield name -> value
    propertyPairs.toMap
  }

  private def parseFailure(isFailure: Boolean)(node: Node): Failure = {
    val message = getAttribute(node, "message").getOrElse(
      throw new ParseException("No 'message' field supplied on <failure/> or <error/>"))
    val typeOpt = getAttribute(node, "type")
    val fullMessageOpt = node.text match {
      case "" ⇒ None
      case s  ⇒ Some(s)
    }
    Failure(
      isFailure = isFailure,
      message = message,
      typeOpt = typeOpt,
      fullMessageOpt = fullMessageOpt)
  }

  private def parseCase(caseNode: Node): TestCase = {
    val timeOpt = getAttribute(caseNode, "time").map(parseDecimal).map(asDuration)
    val name = getAttribute(caseNode, "name").getOrElse(
      throw new ParseException("No 'name' field supplied on <testcase/>"))
    val className = getAttribute(caseNode, "classname").getOrElse(
      throw new ParseException("No 'classname' field supplied on <testcase/>"))

    val failureOpt =
      (caseNode \ "failure").headOption.map(parseFailure(isFailure = true)) orElse
        (caseNode \ "error").headOption.map(parseFailure(isFailure = false))

    TestCase(
      name = name,
      className = className,
      failureOpt = failureOpt,
      timeOpt = timeOpt)
  }

}

case class TestSuite(
  name: String,
  testCases: List[TestCase],
  errorsOpt: Option[Int],
  failuresOpt: Option[Int],
  testsOpt: Option[Int],
  timeOpt: Option[Duration],
  hostnameOpt: Option[String],
  timestampOpt: Option[DateTime],
  properties: Map[String, String],
  sysoutOpt: Option[String],
  syserrOpt: Option[String])

case class TestCase(
  name: String,
  className: String,
  failureOpt: Option[Failure],
  timeOpt: Option[Duration])

case class Failure(
    isFailure: Boolean /* else error */ ,
    message: String,
    typeOpt: Option[String],
    fullMessageOpt: Option[String]) {

  def isError: Boolean = !isFailure

}