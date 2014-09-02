package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.Configuration

/**
 * @param counts for a configuration, ordered from earliest to latest
 */
case class HistoricalTestCountsTimeline(configuration: Configuration, counts: List[HistoricalTestCounts]) {

}