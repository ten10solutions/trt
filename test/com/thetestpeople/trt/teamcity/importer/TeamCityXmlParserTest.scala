package com.thetestpeople.trt.teamcity.importer

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.utils.TestUtils
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.forOffsetHours
import com.thetestpeople.trt.utils.UriUtils._
import org.joda.time.Duration

@RunWith(classOf[JUnitRunner])
class TeamCityXmlParserTest extends FlatSpec with Matchers {

  "Parsing a TeamCity list of builds XML" should "correctly capture the builds" in {
    val parser = new TeamCityXmlParser
    val builds = parser.parseBuildLinks(TestUtils.loadXmlFromClasspath("/teamcity/builds.xml"))

    builds should equal(
      Seq(
        TeamCityBuildLink(id = 145897, number = "0.1.78.209", buildPath = "/app/rest/builds/id:145897", finished = true),
        TeamCityBuildLink(id = 137748, number = "0.1.77.194", buildPath = "/app/rest/builds/id:137748", finished = true),
        TeamCityBuildLink(id = 129542, number = "0.1.70.160", buildPath = "/app/rest/builds/id:129542", finished = true)))
  }

  "Parsing a TeamCity build XML" should "correctly capture build information" in {
    val parser = new TeamCityXmlParser
    val build = parser.parseBuild(TestUtils.loadXmlFromClasspath("/teamcity/build-145897.xml"))

    build.url should equal(uri("https://teamcity.jetbrains.com/viewLog.html?buildId=145897&buildTypeId=NetCommunityProjects_Femah_Commit"))
    build.startDate should equal(new DateTime(2014, 7, 16, 14, 35, 54, forOffsetHours(4)))
    build.finishDate should equal(new DateTime(2014, 7, 16, 14, 36, 46, forOffsetHours(4)))
    build.number should equal("0.1.78.209")
    build.testOccurrencesPathOpt should equal(Some("/httpAuth/app/rest/testOccurrences?locator=build:(id:145897)"))
  }

  "Parsing a TeamCity list of test occurrences XML" should "correctly capture the paths to the test occurrences" in {
    val parser = new TeamCityXmlParser
    val paths = parser.parseTestOccurrences(TestUtils.loadXmlFromClasspath("/teamcity/testOccurrences.xml"))

    paths should equal(Seq(
      "/httpAuth/app/rest/testOccurrences/id:537,build:(id:157593)",
      "/httpAuth/app/rest/testOccurrences/id:541,build:(id:157593)"))
  }

  "Parsing TeamCity test occurrence XML" should "correctly capture the test occurrence details" in {
    val parser = new TeamCityXmlParser
    val testOccurrence = parser.parseTestOccurrence(TestUtils.loadXmlFromClasspath("/teamcity/testOccurrence.xml"))

    TeamCityTestOccurrence(
      testName = "TestSuite: com.turn.ttorrent.common.TorrentTest.torrent_from_multiple_files",
      status = "FAILURE",
      detailOpt = Some("java.lang.AssertionError"),
      durationOpt = Some(Duration.millis(72)))
  }

}