package com.thetestpeople.trt.jenkins.importer

import scala.xml.Elem
import java.net.URI
import com.thetestpeople.trt.utils.UriUtils._

class JenkinsJobXmlParser {

  def parse(xml: Elem): JenkinsJob = {
    val buildUrls = (xml \ "build" \ "url").toList.map(node ⇒ uri(node.text))
    val name = (xml \ "name").headOption.map(_.text).getOrElse(
      throw ParseException(s"Cannot find <name/> element in Jenkins job XML"))
    val url = (xml \ "url").headOption.map(_.text).getOrElse(
      throw ParseException(s"Cannot find <url/> element in Jenkins job XML"))
    JenkinsJob(name = name, url = uri(url), buildUrls = buildUrls)
  }

}