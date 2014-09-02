package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class DescribeDurationTest extends FlatSpec with Matchers {

  import DateUtils.describeDuration

  "describeDuration" should "describe hours and minutes" in {

    describeDuration(1.hour) should equal("1 hour")
    describeDuration(2.hours) should equal("2 hours")
    describeDuration(3.hours + 3.minutes) should equal("3 hours, 3 mins")
    describeDuration(3.hours + 3.minutes + 3.seconds) should equal("3 hours, 3 mins")
    describeDuration(4.hours + 55.minutes) should equal("4 hours, 55 mins")
    describeDuration(23.hours + 59.minutes) should equal("23 hours, 59 mins")

  }

  it should "describe minutes and seconds" in {

    describeDuration(1.minute) should equal("1 min")
    describeDuration(2.minutes) should equal("2 mins")
    describeDuration(3.minutes + 20.seconds) should equal("3 mins, 20 s")
    describeDuration(3.minutes + 20.seconds + 200.millis) should equal("3 mins, 20 s")

  }

  it should "describe seconds and millis" in {

    describeDuration(1.second) should equal("1 s")
    describeDuration(2.seconds) should equal("2 s")
    describeDuration(1.second + 200.millis) should equal("1.2 s")
    describeDuration(1.second + 201.millis) should equal("1.2 s")
    describeDuration(1.second + 2.millis) should equal("1.0 s")

  }

  it should "describe millis" in {

    describeDuration(100.millis) should equal("100 ms")
    describeDuration(123.millis) should equal("123 ms")
    describeDuration(12.millis) should equal("12 ms")
    describeDuration(1.millis) should equal("1 ms")
    describeDuration(0.millis) should equal("0 ms")

  }

}