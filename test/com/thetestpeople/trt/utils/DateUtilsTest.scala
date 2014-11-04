package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.thetestpeople.trt.utils.UriUtils._
import com.github.nscala_time.time.Imports._
import org.joda.time.format.ISODateTimeFormat

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

  private def date(s: String): DateTime = ISODateTimeFormat.dateTimeParser.parseDateTime(s)

  "Getting all days in an interval" should "work when the finish date is an earlier hour in the day than the start date" in {
    val interval = date("2014-11-01T07:59:32Z") to date("2014-11-03T22:59:32Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z"),
      date("2014-11-03T00:00:00Z"),
      date("2014-11-04T00:00:00Z")).map(_.getMillis))
  }

  it should "work when the start date is an earlier hour in the day than the finish date" in {
    val interval = date("2014-11-01T22:59:32Z") to date("2014-11-03T07:59:32Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z"),
      date("2014-11-03T00:00:00Z"),
      date("2014-11-04T00:00:00Z")).map(_.getMillis))
  }

  it should "get a single date if the interval is within one day" in {
    val interval = date("2014-11-01T07:59:32Z") to date("2014-11-01T22:59:32Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z")).map(_.getMillis))
  }

  it should "get a single date if the interval is empty" in {
    val interval = date("2014-11-01T07:59:32Z") to date("2014-11-01T07:59:32Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z")).map(_.getMillis))
  }

  it should "work if the start date is on midnight" in {
    val interval = date("2014-11-01T00:00:00Z") to date("2014-11-03T07:59:32Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z"),
      date("2014-11-03T00:00:00Z"),
      date("2014-11-04T00:00:00Z")).map(_.getMillis))
  }

  it should "work if the finish date is on midnight" in {
    val interval = date("2014-11-01T07:59:00Z") to date("2014-11-03T00:00:00Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z"),
      date("2014-11-03T00:00:00Z"),
      date("2014-11-04T00:00:00Z")).map(_.getMillis))
  }

  it should "work if both dates are on midnight" in {
    val interval = date("2014-11-01T00:00:00Z") to date("2014-11-03T00:00:00Z")
    val days = DateUtils.getAllDaysIn(interval, DateTimeZone.UTC)
    days.map(_.getMillis) should equal(Seq(
      date("2014-11-02T00:00:00Z"),
      date("2014-11-03T00:00:00Z"),
      date("2014-11-04T00:00:00Z")).map(_.getMillis))
  }

}