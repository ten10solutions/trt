package com.thetestpeople.trt.teamcity.importer

import scala.xml.Elem
import org.joda.time.Duration
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import scala.xml.XML
import com.thetestpeople.trt.utils.TestUtils
import com.thetestpeople.trt.utils.UriUtils._
import java.net.URI
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.scalactic.Equality

@RunWith(classOf[JUnitRunner])
class TeamCityBuildXmlParserTest extends FlatSpec with Matchers {

  "Parsing a TeamCity build XML" should "correctly capture build information" in {
    val parser = new TeamCityBuildXmlParser()
    val build = parser.parse(TestUtils.loadXmlFromClasspath("/teamcity/build-145897.xml"))

    build.startDate should equal(new DateTime(2014, 7, 16, 14, 35, 54, DateTimeZone.forOffsetHours(4)))
    build.finishDate should equal(new DateTime(2014, 7, 16, 14, 36, 46, DateTimeZone.forOffsetHours(4)))
    build.number should equal("0.1.78.209")
    build.testOccurrencesPathOpt should equal (Some("/httpAuth/app/rest/testOccurrences?locator=build:(id:145897)"))
  }
}
