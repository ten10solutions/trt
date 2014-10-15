package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.CiImportSpec

object FakeJenkinsImportQueue extends JenkinsImportQueue {

  def add(importSpecId: Id[CiImportSpec]) {}

}