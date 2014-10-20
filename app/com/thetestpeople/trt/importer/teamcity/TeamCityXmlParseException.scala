package com.thetestpeople.trt.importer.teamcity

case class TeamCityXmlParseException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)