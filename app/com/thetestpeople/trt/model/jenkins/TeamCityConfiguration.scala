package com.thetestpeople.trt.model.jenkins

import java.net.URI
import com.thetestpeople.trt.utils.http.Credentials

case class TeamCityConfiguration(
    usernameOpt: Option[String] = None,
    passwordOpt: Option[String] = None) {

  def credentialsOpt = for (username ← usernameOpt; password ← passwordOpt) yield Credentials(username, password)

}