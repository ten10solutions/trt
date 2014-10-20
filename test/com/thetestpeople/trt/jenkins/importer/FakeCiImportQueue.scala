package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.CiImportSpec
import com.thetestpeople.trt.importer.CiImportQueue

object FakeCiImportQueue extends CiImportQueue {

  def add(importSpecId: Id[CiImportSpec]) {}

}