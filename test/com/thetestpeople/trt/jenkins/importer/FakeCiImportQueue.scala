package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.CiImportSpec

object FakeCiImportQueue extends CiImportQueue {

  def add(importSpecId: Id[CiImportSpec]) {}

}