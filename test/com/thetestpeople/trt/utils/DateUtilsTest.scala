package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.thetestpeople.trt.utils.UriUtils._
import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class DateUtilsTest extends FlatSpec with Matchers {

  "Sampling times across an interval" should "include the first and last points" in {
    val earliest = 2.days.ago
    val latest = 1.day.ago
    DateUtils.sampleTimesBetween(earliest to latest, samples = 2) should equal(List(earliest, latest))
  }

  it should "return the correct number of samples, all-distinct, in oldest to most recent order" in {
    val earliest = 2.days.ago
    val latest = 1.day.ago
    val samples = DateUtils.sampleTimesBetween(earliest to latest, samples = 10)
    samples.size should equal(10)
    samples.distinct.size should equal(10)
    samples.sorted should equal(samples)
  }
  
  it should "not return duplicate samples" in {
    val time = 1.day.ago
    DateUtils.sampleTimesBetween(time to time, samples = 10)
  }
  

}