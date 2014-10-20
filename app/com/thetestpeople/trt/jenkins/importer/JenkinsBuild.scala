package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.service.Incoming
import org.joda.time.DateTime
import java.net.URI

case class JenkinsBuild(
  jobUrl: URI,
  buildSummary: JenkinsBuildSummary,
  testResult: OrdinaryTestResult,
  consoleTextOpt: Option[String])

