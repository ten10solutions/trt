package com.thetestpeople.trt.teamcity.importer

import org.joda.time.DateTime

case class TeamCityBuild(
    startDate: DateTime,
    finishDate: DateTime,
    number: String,
    testOccurrencesPathOpt: Option[String]) {

}