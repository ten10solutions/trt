package com.thetestpeople.trt.jenkins.importer

import org.joda.time.Duration
import org.joda.time.DateTime
import scala.xml.Elem
import scala.xml.Node
import java.net.URI

class JenkinsBuildXmlParser {

  @throws[ParseException]
  def parseBuild(elem: Elem): JenkinsBuildSummary = {
    val url = getFieldOpt(elem, "url").getOrElse(
      throw new ParseException("Could not find a <url> element"))
    val durationOpt = getFieldOpt(elem, "duration").map(parseLong).map(Duration.millis)
    val nameOpt = getFieldOpt(elem, "fullDisplayName")
    val timestampOpt = getFieldOpt(elem, "timestamp").map(parseLong).map(new DateTime(_))
    val resultOpt = getFieldOpt(elem, "result")
    val hasTestReport = (elem \ "action" \ "urlName").exists(_.text == "testReport")
    val isBuilding = getFieldOpt(elem, "building").getOrElse(
      throw new ParseException("Could not find a <building> element")).toBoolean

    JenkinsBuildSummary(
      url = new URI(url),
      durationOpt = durationOpt,
      nameOpt = nameOpt,
      timestampOpt = timestampOpt,
      resultOpt = resultOpt,
      hasTestReport = hasTestReport,
      isBuilding = isBuilding)
  }

  private def getFieldOpt(node: Node, name: String): Option[String] =
    (node \ name).headOption.map(_.text)

  private def parseLong(s: String): Long =
    try s.toLong
    catch {
      case e: NumberFormatException ⇒
        throw ParseException(s"Cannot parse '$s' as an integer", e)
    }

}