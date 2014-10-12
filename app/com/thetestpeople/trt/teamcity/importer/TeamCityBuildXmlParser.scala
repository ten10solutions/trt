package com.thetestpeople.trt.teamcity.importer

import scala.xml._

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class TeamCityBuildXmlParser {

  private val dateParser = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ").withOffsetParsed

  def parse(elem: Elem): TeamCityBuild = {
    val startDate = parseDate(getField(elem, "startDate"))
    val finishDate = parseDate(getField(elem, "finishDate"))
    val hasTests = (elem \ "testOccurrences").nonEmpty
    val testOccurrencesPathOpt = (elem \ "testOccurrences" \ "@href").headOption.map(_.text)
    val number = getField(elem, "@number")
    TeamCityBuild(
      startDate = startDate,
      finishDate = finishDate,
      number = number,
      testOccurrencesPathOpt = testOccurrencesPathOpt)
  }

  private def parseDate(s: String): DateTime =
    try
      dateParser.parseDateTime(s)
    catch {
      case e: IllegalArgumentException ⇒
        throw new TeamCityXmlParseException(s"Could not parse '$s' as a date/time", e)
    }

  private def getField(node: Node, name: String): String =
    getFieldOpt(node, name).getOrElse(
      throw new TeamCityXmlParseException(s"Could not find field '$name' in build XML"))

  private def getFieldOpt(node: Node, name: String): Option[String] =
    (node \ name).headOption.map(_.text)
}