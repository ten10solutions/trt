package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins._

case class FullJenkinsConfiguration(config: JenkinsConfiguration, params: List[JenkinsJobParam])
