package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins.CiImportSpec

case class BatchAndLog(
    batch: Batch, 
    logOpt: Option[String], 
    importSpecIdOpt: Option[Id[CiImportSpec]], 
    commentOpt: Option[String])
