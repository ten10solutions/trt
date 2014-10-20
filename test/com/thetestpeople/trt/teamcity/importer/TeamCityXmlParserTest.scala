package com.thetestpeople.trt.importer.teamcity

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.utils.TestUtils
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.forOffsetHours
import com.thetestpeople.trt.utils.UriUtils._
import org.joda.time.Duration
import scala.xml.Elem

@RunWith(classOf[JUnitRunner])
class TeamCityXmlParserTest extends FlatSpec with Matchers {

  val parser = new TeamCityXmlParser

  "Parsing TeamCity build type XML" should "correctly capture the information" in {
    val buildType = parser.parseBuildType(getXml("buildType.xml"))
    buildType.name should equal("Build")
    buildType.projectName should equal("Test Reporty Thing")
    buildType.buildsPathOpt should equal(Some("/guestAuth/app/rest/buildTypes/id:TestReportyThing_Build/builds/"))
  }

  "Parsing a TeamCity list of builds XML" should "correctly capture the builds" in {
    val builds = parser.parseBuildLinks(getXml("builds.xml"))

    builds should equal(
      Seq(
        TeamCityBuildLink(id = 145897, number = "0.1.78.209", buildPath = "/app/rest/builds/id:145897", finished = true,
          webUrl = uri("https://teamcity.jetbrains.com/viewLog.html?buildId=145897&buildTypeId=NetCommunityProjects_Femah_Commit")),
        TeamCityBuildLink(id = 137748, number = "0.1.77.194", buildPath = "/app/rest/builds/id:137748", finished = true,
          webUrl = uri("https://teamcity.jetbrains.com/viewLog.html?buildId=137748&buildTypeId=NetCommunityProjects_Femah_Commit")),
        TeamCityBuildLink(id = 129542, number = "0.1.70.160", buildPath = "/app/rest/builds/id:129542", finished = true,
          webUrl = uri("https://teamcity.jetbrains.com/viewLog.html?buildId=129542&buildTypeId=NetCommunityProjects_Femah_Commit"))))
  }

  "Parsing a TeamCity build XML" should "correctly capture build information" in {
    val build = parser.parseBuild(getXml("build-145897.xml"))

    build.url should equal(uri("https://teamcity.jetbrains.com/viewLog.html?buildId=145897&buildTypeId=NetCommunityProjects_Femah_Commit"))
    build.state should equal("finished")
    build.startDate should equal(new DateTime(2014, 7, 16, 14, 35, 54, forOffsetHours(4)))
    build.finishDate should equal(new DateTime(2014, 7, 16, 14, 36, 46, forOffsetHours(4)))
    build.number should equal("0.1.78.209")
    build.testOccurrencesPathOpt should equal(Some("/httpAuth/app/rest/testOccurrences?locator=build:(id:145897)"))
  }

  "Parsing a TeamCity list of test occurrences XML" should "correctly capture the paths to the test occurrences" in {
    val TeamCityTestOccurrences(Some(nextPagePath), paths) = parser.parseTestOccurrences(getXml("testOccurrences.xml"))

    nextPagePath should equal("/httpAuth/app/rest/testOccurrences?locator=count:100,start:200,build:(id:157593)")
    paths should equal(Seq(
      "/httpAuth/app/rest/testOccurrences/id:537,build:(id:157593)",
      "/httpAuth/app/rest/testOccurrences/id:541,build:(id:157593)"))
  }

  "Parsing TeamCity test occurrence XML" should "correctly capture the test occurrence details" in {
    val testOccurrence = parser.parseTestOccurrence(getXml("testOccurrence.xml"))

    TeamCityTestOccurrence(
      testName = "TestSuite: com.turn.ttorrent.common.TorrentTest.torrent_from_multiple_files",
      status = "FAILURE",
      detailOpt = Some("java.lang.AssertionError"),
      durationOpt = Some(Duration.millis(72)))
  }

  private def getXml(filename: String): Elem = TestUtils.loadXmlFromClasspath("/teamcity/" + filename)
  
}