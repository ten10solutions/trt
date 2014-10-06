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
      "https://builds.apache.org/view/All/job/flume-trunk/666/",
      "https://builds.apache.org/view/All/job/flume-trunk/665/",
      "https://builds.apache.org/view/All/job/flume-trunk/664/",
      "https://builds.apache.org/view/All/job/flume-trunk/663/").map(uri)

    buildUrls should contain theSameElementsAs expectedUrls
    jobName should equal("flume-trunk")
    url should equal(uri("https://builds.apache.org/view/All/job/flume-trunk/"))
  }

  private def parse(xml: Elem): JenkinsJob = new JenkinsJobXmlParser().parse(xml)

}