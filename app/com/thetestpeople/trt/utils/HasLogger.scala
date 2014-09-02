package com.thetestpeople.trt.utils

import play.api.Logger

trait HasLogger {

  val logger = Logger(getClass)

}
