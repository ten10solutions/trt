package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.Configuration
import org.joda.time.DateTime
import org.joda.time.Interval

/**
 * @param counts for a configuration, ordered from earliest to latest
 */
case class HistoricalTestCountsTimeline(configuration: Configuration, counts: Seq[HistoricalTestCounts]) {

  def earliestTime: DateTime = counts.head.when

  def latestTime: DateTime = counts.last.when
  
  def interval = new Interval(earliestTime, latestTime)
  
}