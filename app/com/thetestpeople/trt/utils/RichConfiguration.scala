package com.thetestpeople.trt.utils

import scala.concurrent.duration._
import play.api.Configuration
import scala.concurrent.duration.FiniteDuration

object RichConfiguration {

  implicit class RichConfig(configuration: Configuration) {

    def getDuration(key: String, default: FiniteDuration): FiniteDuration =
      configuration.getMilliseconds(key).map(_.millis).getOrElse(default)

  }

}