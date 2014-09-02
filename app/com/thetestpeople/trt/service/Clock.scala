package com.thetestpeople.trt.service

import org.joda.time.DateTime

trait Clock {

  def now: DateTime

}

object SystemClock extends Clock {

  def now = new DateTime

}