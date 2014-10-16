package com.thetestpeople.trt.teamcity.importer

import java.net.URI

case class TeamCityBuildLink(id: Int, number: String, finished: Boolean, buildPath: String, webUrl: URI)
