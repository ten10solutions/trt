package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import java.net.URI

case class JenkinsJob(
  id: Id[JenkinsJob] = Id.dummy,
  url: URI,
  name: String) extends EntityType