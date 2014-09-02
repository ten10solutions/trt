package com.thetestpeople.trt.jenkins.importer

import org.joda.time.Duration
import org.joda.time.DateTime
import java.net.URI

case class BuildSummary(
  url: URI,
  durationOpt: Option[Duration],
  nameOpt: Option[String],
  timestampOpt: Option[DateTime],
  result: String,
  hasTestReport: Boolean)
