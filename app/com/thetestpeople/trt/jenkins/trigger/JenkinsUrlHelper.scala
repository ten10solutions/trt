package com.thetestpeople.trt.jenkins.trigger

import java.net.URI

object JenkinsUrlHelper {

  def getServerUrl(jobUrl: URI) = {
    val jobUrlString = jobUrl.toString
    jobUrlString.lastIndexOf("/job/") match {
      case -1 ⇒ throw new RuntimeException(s"Rerun URL is not a Jenkins job: $jobUrlString")
      case i  ⇒ new URI(jobUrlString.substring(0, i))
    }
  }

}