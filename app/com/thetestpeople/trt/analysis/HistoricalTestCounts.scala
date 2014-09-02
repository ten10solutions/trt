package com.thetestpeople.trt.analysis

import org.joda.time.DateTime
import com.thetestpeople.trt.model.TestCounts

/**
 * Test counts at a point in time
 */
case class HistoricalTestCounts(when: DateTime, testCounts: TestCounts)
