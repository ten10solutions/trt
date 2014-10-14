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
class TeamCityUrlParserTest extends FlatSpec with Matchers {

  "TeamCity URL parser" should "find the build configuration and server URL" in {
    val teamCityUrl = uri("https://teamcity.jetbrains.com/viewType.html?buildTypeId=NetCommunityProjects_Femah_Commit")

    val Right(TeamCityConfiguration(serverUrl, buildTypeId)) = TeamCityUrlParser.parse(teamCityUrl)

    serverUrl should equal(uri("https://teamcity.jetbrains.com"))
    buildTypeId should equal("NetCommunityProjects_Femah_Commit")
  }

  it should "give an error on a non-TeamCity URL" in {
    val Left(_) = TeamCityUrlParser.parse(uri("http://www.google.com"))
  }

}