package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import org.joda.time.DateTime
import java.net.URI

case class CiBuild(
  batchId: Id[Batch],
  importTime: DateTime,
  buildUrl: URI,
  buildNumber: Int,
  jobId: Id[CiJob],
  importSpecIdOpt: Option[Id[CiImportSpec]])