package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.service.Incoming
import org.joda.time.DateTime
import java.net.URI

case class JenkinsBuild(
  jobUrl: URI,
  buildSummary: BuildSummary,
  testResult: OrdinaryTestResult,
  consoleTextOpt: Option[String])

