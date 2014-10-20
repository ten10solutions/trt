package com.thetestpeople.trt.importer.jenkins

import scala.xml.Elem
import com.thetestpeople.trt.utils.UriUtils._
import scala.xml.Node

class JenkinsJobXmlParser {

  def parse(xml: Elem): JenkinsJob = {
    val buildLinks = (xml \ "allBuild").map(parseBuildLink)
    val name = (xml \ "name").headOption.map(_.text).getOrElse(
      throw ParseException(s"Cannot find <name> element in Jenkins job XML"))
    val url = (xml \ "url").headOption.map(_.text).getOrElse(
      throw ParseException(s"Cannot find <url> element in Jenkins job XML"))
    JenkinsJob(name = name, url = uri(url), buildLinks = buildLinks)
  }

  def parseBuildLink(node: Node): JenkinsBuildLink = {
    val url = uri((node \ "url").headOption.map(_.text).getOrElse(
      throw ParseException(s"Cannot find <url> element underneath <build> in Jenkins job XML")))
    val buildNumber = (node \ "number").headOption.map(_.text.toInt).getOrElse(
      throw ParseException(s"Cannot find <number> element underneath <build> in Jenkins job XML"))
    JenkinsBuildLink(url, buildNumber)
  }

}