package com.thetestpeople.trt.importer.teamcity

import com.thetestpeople.trt.utils.UriUtils._
import java.net.URI

case class TeamCityJobLink(serverUrl: URI, buildTypeId: String) {

  def relativePath(path: String): URI = uri(serverUrl.toString + path)

}

