package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import java.net.URI

case class CiJob(
  id: Id[CiJob] = Id.dummy,
  url: URI,
  name: String) extends EntityType