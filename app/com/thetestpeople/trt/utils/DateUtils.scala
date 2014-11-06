package com.thetestpeople.trt.utils

import org.joda.time.DateTime
import com.ocpsoft.pretty.time.PrettyTime
import org.joda.time.Duration
import java.math.MathContext
import org.joda.time.Interval
import com.github.nscala_time.time.Imports._
import scala.collection.mutable.ListBuffer

object DateUtils {

  def describeRelative(dateTime: DateTime) = new PrettyTime().format(dateTime.toDate)

  def describeDuration(duration: Duration): String = {
    val hours = duration.getStandardHours
    val minutes = duration.getStandardMinutes
    val seconds = duration.getStandardSeconds
    def minutesWord(minutes: Long) = if (minutes == 1) "min" else "mins"
    def hoursWord(hours: Long) = if (hours == 1) "hour" else "hours"
    if (hours > 0) {
      val remainder = duration.minus(Duration.standardHours(hours))
      val minutes = remainder.getStandardMinutes
      if (minutes > 0)
        s"$hours ${hoursWord(hours)}, $minutes ${minutesWord(minutes)}"
      else
        s"$hours ${hoursWord(hours)}"
    } else if (minutes > 0) {
      if (minutes < 10) {
        val remainder = duration.minus(Duration.standardMinutes(minutes))
        val seconds = remainder.getStandardSeconds
        if (seconds > 0)
          s"$minutes ${minutesWord(minutes)}, ${seconds} s"
        else
          s"$minutes ${minutesWord(minutes)}"
      } else
        s"$minutes ${minutesWord(minutes)}"
    } else if (seconds > 0) {
      val remainder = duration.minus(Duration.standardSeconds(seconds))
      val millis = remainder.getMillis()
      if (millis == 0)
        s"$seconds s"
      else {
        val secondsDecimal = BigDecimal(seconds + (millis / 1000.0)).round(new MathContext(2))
        s"$secondsDecimal s"
      }
    } else
      s"${duration.getMillis} ms"
  }

  /**
   * For each day in the interval, return the DateTime at the start of the day. Includes a final time for midnight after
   * the end of the interval.
   */
  def getAllDaysIn(interval: Interval, timeZone: DateTimeZone = DateTimeZone.getDefault): Seq[DateTime] =
    getAllDaysBetween(interval.start.toLocalDate, interval.end.toLocalDate).map(_.toDateTimeAtStartOfDay(timeZone))

  private def getAllDaysBetween(start: LocalDate, end: LocalDate): Seq[LocalDate] = {
    val dates = ListBuffer[LocalDate]()
    var current = start
    while (current <= end) {
      current += 1.day
      dates += current
    }
    dates
  }

  /**
   * @return points in time evenly spaced across the given interval, including the start and end times. Fewer than
   *  the requested number of samples will be returned if there are not enough distinct times in the interval.
   */
  def sampleTimesBetween(interval: Interval, samples: Int): Seq[DateTime] =
    sampleTimesBetween(interval.start, interval.end, samples)

  def sampleTimesBetween(start: DateTime, end: DateTime, samples: Int): Seq[DateTime] = {
    require(samples >= 2)
    val gapMillis = (end.getMillis - start.getMillis) / (samples - 1)
    val milliOffsets: Seq[Long] = (0 until (samples - 1)).map(_ * gapMillis)
    (milliOffsets.map(start + _) :+ end).distinct
  }

}