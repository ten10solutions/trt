package com.thetestpeople.trt.utils

import scala.util.control.Exception._
import org.joda.time.format.PeriodFormat
import org.joda.time.format.PeriodFormatter
import org.joda.time.Duration

case class DurationParser(periodFormatter: PeriodFormatter = PeriodFormat.getDefault) {

  def parse(s: String): Option[Duration] =
    catching(classOf[IllegalArgumentException]).opt {
      periodFormatter.parsePeriod(s).toStandardDuration
    }

  def asString(duration: Duration): String =
    periodFormatter.print(duration.toPeriod)

}
