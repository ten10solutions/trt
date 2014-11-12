package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins.CiImportSpec

case class EnrichedBatch(
  batch: Batch,
  executions: Seq[EnrichedExecution] = Seq(),
  logOpt: Option[String] = None,
  importSpecIdOpt: Option[Id[CiImportSpec]] = None,
  commentOpt: Option[String] = None)
