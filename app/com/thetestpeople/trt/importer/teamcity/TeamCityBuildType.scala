package com.thetestpeople.trt.importer.teamcity

import java.net.URI

case class TeamCityBuildType(name: String, projectName: String, buildsPathOpt: Option[String], webUrl: URI)
