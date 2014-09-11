package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import org.joda.time.DateTime
import java.net.URI

case class JenkinsBuild(
  batchId: Id[Batch],
  importTime: DateTime,
  buildUrl: URI,
  buildNumber: Int,
  jobId: Id[JenkinsJob])