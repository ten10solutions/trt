package com.thetestpeople.trt.utils

import org.joda.time.DateTime
import com.ocpsoft.pretty.time.PrettyTime
import org.joda.time.Duration
import java.math.MathContext
import org.joda.time.Interval
import com.github.nscala_time.time.Imports._

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
        //        val secondsDecimal = BigDecimal(s"${seconds}.${millis}").round(new MathContext(2))
        s"$secondsDecimal s"
      }
    } else
      s"${duration.getMillis} ms"
  }

  /**
   * @return points in time evenly spaced across the given interval, including the start and end times. Fewer than 
   *  the requested number of samples will be returned if there are not enough distinct times in the interval.
   */
  def sampleTimesBetween(interval: Interval, samples: Int): List[DateTime] =
    sampleTimesBetween(interval.start, interval.end, samples)

  def sampleTimesBetween(start: DateTime, end: DateTime, samples: Int): List[DateTime] = {
    require(samples >= 2)
    val gapMillis = (end.getMillis - start.getMillis) / (samples - 1)
    val milliOffsets: List[Long] = (0 until (samples - 1)).map(_ * gapMillis).toList
    (milliOffsets.map(start + _) :+ end).distinct
  }

}