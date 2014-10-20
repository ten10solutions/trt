package com.thetestpeople.trt.importer.teamcity

import java.net.URI

case class TeamCityBuildLink(id: Int, number: String, finished: Boolean, buildPath: String, webUrl: URI)
