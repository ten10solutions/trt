package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins.JenkinsImportSpec

case class BatchAndLog(batch: Batch, logOpt: Option[String], importSpecIdOpt: Option[Id[JenkinsImportSpec]], commentOpt: Option[String])
