package com.thetestpeople.trt.analysis

import org.joda.time.Duration

/**
 * Information about a period of one or more consecutive passes or failures.
 *
 * For example: 3 consecutive passes over a period of a day.
 */
case class PassFailBlock(count: Int, passed: Boolean, duration: Duration)
