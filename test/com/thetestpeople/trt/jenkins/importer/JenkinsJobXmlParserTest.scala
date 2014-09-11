package com.thetestpeople.trt.jenkins.importer

import scala.xml.Elem
import org.joda.time.Duration
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import scala.xml.XML
import com.thetestpeople.trt.utils.TestUtils
import com.thetestpeople.trt.utils.UriUtils._
import java.net.URI

@RunWith(classOf[JUnitRunner])
class JenkinsJobXmlParserTest extends FlatSpec with Matchers {

  "Parsing Jenkins project XML" should "correctly capture the build URLs" in {

    val JenkinsJob(jobName, url, buildLinks) = parse(TestUtils.loadXmlFromClasspath("/freeStyleProject.xml"))
    val buildUrls = buildLinks.map(_.buildUrl)

    val expectedUrls = List(
      "http://ci.pentaho.com/job/pentaho-big-data-plugin/758/",
      "http://ci.pentaho.com/job/pentaho-big-data-plugin/757/",
      "http://ci.pentaho.com/job/pentaho-big-data-plugin/756/",
      "http://ci.pentaho.com/job/pentaho-big-data-plugin/57/").map(uri)

    buildUrls should contain theSameElementsAs expectedUrls
    jobName should equal("pentaho-big-data-plugin")
    url should equal(uri("http://ci.pentaho.com/job/pentaho-big-data-plugin/"))
  }

  private def parse(xml: Elem): JenkinsJob = new JenkinsJobXmlParser().parse(xml)

}