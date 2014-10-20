package com.thetestpeople.trt.importer.jenkins

import java.net.URI

case class JenkinsBuild(
  jobUrl: URI,
  buildSummary: JenkinsBuildSummary,
  testResult: OrdinaryTestResult,
  consoleTextOpt: Option[String])

