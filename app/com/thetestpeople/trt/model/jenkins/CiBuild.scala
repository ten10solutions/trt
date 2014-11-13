package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import org.joda.time.DateTime
import java.net.URI

/**
 * A build of a CI job (@see CiJob) that has been imported into trt.
 */
case class CiBuild(
  batchId: Id[Batch],
  importTime: DateTime,
  buildUrl: URI,
  buildNumberOpt: Option[Int],
  buildNameOpt: Option[String],
  jobId: Id[CiJob],
  importSpecIdOpt: Option[Id[CiImportSpec]])