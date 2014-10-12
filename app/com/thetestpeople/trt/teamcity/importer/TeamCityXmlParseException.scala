package com.thetestpeople.trt.teamcity.importer

case class TeamCityXmlParseException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)