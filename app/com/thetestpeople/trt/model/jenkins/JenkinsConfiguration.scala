package com.thetestpeople.trt.model.jenkins

import java.net.URI
import com.thetestpeople.trt.utils.http.Credentials

case class JenkinsConfiguration(
    usernameOpt: Option[String] = None,
    apiTokenOpt: Option[String] = None,
    rerunJobUrlOpt: Option[URI] = None,
    authenticationTokenOpt: Option[String] = None) {

  def credentialsOpt = for (username ← usernameOpt; apiToken ← apiTokenOpt) yield Credentials(username, apiToken)

}