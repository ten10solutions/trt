package com.thetestpeople.trt.teamcity.importer

import org.joda.time.DateTime
import java.net.URI

case class TeamCityBuild(
    url: URI,
    state: String,
    startDate: DateTime,
    finishDate: DateTime,
    number: String,
    testOccurrencesPathOpt: Option[String],
    occurrences: Seq[TeamCityTestOccurrence] = Seq()) {

  def finished: Boolean = state == "finished"

}