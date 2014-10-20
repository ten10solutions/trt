package com.thetestpeople.trt.teamcity.importer

import java.net.URI

case class TeamCityBuildType(name: String, projectName: String, buildsPathOpt: Option[String], webUrl: URI)
