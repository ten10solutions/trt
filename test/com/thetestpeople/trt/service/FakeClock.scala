package com.thetestpeople.trt.service

import org.joda.time.DateTime
import org.joda.time.Duration
import com.github.nscala_time.time.Imports._

case class FakeClock(private var _now: DateTime = new DateTime) extends Clock {

  def now = synchronized { _now }

  def now_=(time: DateTime) = synchronized { _now = time }

  def +=(duration: Duration) = now += duration

}