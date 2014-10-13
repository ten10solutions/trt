package com.thetestpeople.trt.teamcity.importer

case class TeamCityTestOccurrence(testName: String, status: String, detailOpt: Option[String], duration: Int) 
