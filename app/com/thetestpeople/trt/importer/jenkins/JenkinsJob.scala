package com.thetestpeople.trt.importer.jenkins

import java.net.URI

case class JenkinsJob(name: String, url: URI, buildLinks: Seq[JenkinsBuildLink])

