package com.thetestpeople.trt.jenkins.importer

import java.net.URI

case class JenkinsJob(name: String, url: URI, buildLinks: Seq[JenkinsBuildLink])

