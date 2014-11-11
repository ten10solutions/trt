package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.Configuration
import org.joda.time.Interval
import com.thetestpeople.trt.utils.DateUtils

case class AllHistoricalTestCounts(historicalTestCountsByConfig: Map[Configuration, HistoricalTestCountsTimeline]) {

  def getHistoricalTestCounts(configuration: Configuration): Option[HistoricalTestCountsTimeline] =
    historicalTestCountsByConfig.get(configuration)

  def interval: Option[Interval] =
    historicalTestCountsByConfig.values.map(_.interval).reduceOption(DateUtils.mergeIntervals)

}