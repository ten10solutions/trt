package com.thetestpeople.trt.teamcity.importer

import scala.xml.Elem
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import scala.xml.Node

class TeamCityXmlParser {

  private val dateParser = DateTimeFormat.forPattern("yyyyMMdd'T'HHmmssZ").withOffsetParsed

  def parseBuild(elem: Elem): TeamCityBuild = {
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

  def parseBuildLinks(elem: Elem): Seq[TeamCityBuildLink] = (elem \ "build").map(parseBuildLink)

  private def parseBuildLink(node: Node): TeamCityBuildLink =
    TeamCityBuildLink(
      id = getField(node, "@id").toInt,
      number = getField(node, "@number"),
      buildPath = getField(node, "@href"),
      finished = getField(node, "@state") == "finished")

  def parseTestOccurrences(elem: Elem): Seq[String] = (elem \ "testOccurrence").flatMap(_ \ "@href" map (_.text))

  def parseTestOccurrence(elem: Elem): TeamCityTestOccurrence =
    TeamCityTestOccurrence(
      testName = (elem \ "test" \ "@name").head.text,
      status = getField(elem, "@status"),
      detailOpt = getFieldOpt(elem, "details"),
      duration = getField(elem, "@duration").toInt)

  private def parseDate(s: String): DateTime =
    try
      dateParser.parseDateTime(s)
    catch {
      case e: IllegalArgumentException ⇒
        throw new TeamCityXmlParseException(s"Could not parse '$s' as a date/time", e)
    }

  private def getField(node: Node, name: String): String =
    getFieldOpt(node, name).getOrElse(
      throw new TeamCityXmlParseException(s"Could not find field '$name' in TeamCity XML"))

  private def getFieldOpt(node: Node, name: String): Option[String] =
    (node \ name).headOption.map(_.text)
}