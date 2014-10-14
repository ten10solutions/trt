package com.thetestpeople.trt.jenkins.importer

import scala.xml._
import org.joda.time._
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.utils.TestUtils
import java.net.URI
import com.thetestpeople.trt.utils.UriUtils._

@RunWith(classOf[JUnitRunner])
class JenkinsBuildXmlParserTest extends FlatSpec with Matchers {

  "Parsing a freestyle build" should "work" in {
    val xml = TestUtils.loadXmlFromClasspath("/freeStyleBuild.xml")
    val buildSummary = parse(xml)

    buildSummary.durationOpt should be(Some(millis(397924)))
    buildSummary.hasTestReport should be(true)
    buildSummary.nameOpt should be(Some("pentaho-big-data-plugin #755"))
    buildSummary.resultOpt should be(Some("UNSTABLE"))
    buildSummary.timestampOpt should be(Some(new DateTime(1392394912000L)))
    buildSummary.url should be(uri("http://ci.pentaho.com/job/pentaho-big-data-plugin/755/"))
    buildSummary.isBuilding should be(false)
  }

  "Parsing a matrix build" should "work" in {
    val xml = TestUtils.loadXmlFromClasspath("/matrixBuild.xml")

    val buildSummary = parse(xml)

    buildSummary.durationOpt should be(Some(millis(2302595)))
    buildSummary.hasTestReport should be(true)
    buildSummary.nameOpt should be(Some("High Level Tests #886"))
    buildSummary.resultOpt should be(Some("UNSTABLE"))
    buildSummary.timestampOpt should be(Some(new DateTime(1392277044928L)))
    buildSummary.url should be(uri("http://asdf:8080/job/highlevel/886/"))
    buildSummary.isBuilding should be(false)
  }

  private def parse(xml: Elem): BuildSummary = new JenkinsBuildXmlParser().parseBuild(xml)

  private def millis(n: Int): Duration = n.millis

}