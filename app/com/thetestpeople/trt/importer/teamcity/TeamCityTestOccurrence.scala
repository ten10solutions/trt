package com.thetestpeople.trt.importer.teamcity

import org.joda.time.Duration

case class TeamCityTestOccurrence(testName: String, status: String, detailOpt: Option[String], durationOpt: Option[Duration]) {
  
  def passed = status == "SUCCESS"
  
} 
