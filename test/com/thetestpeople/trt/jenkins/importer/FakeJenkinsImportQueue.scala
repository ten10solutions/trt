package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.JenkinsImportSpec

object FakeJenkinsImportQueue extends JenkinsImportQueue {

  def add(importSpecId: Id[JenkinsImportSpec]) {}

}