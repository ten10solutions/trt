package com.thetestpeople.trt.analysis

import org.joda.time.DateTime

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._

/**
 * Helper class to quickly calculate the status of a test at multiple points in time.
 *
 * The intention is to repeatedly prune executions by using "ignoreExecutionsAfter()" then "getMostRecentPassFailBlock".
 *
 * @param executions -- sorted most recent first
 */
class QuickTestAnalyser(executions: Array[ExecutionLite]) {

  private var start: Int = 0

  /**
   * Filter out any executions that occurred after the given cutoff time.
   */
  def ignoreExecutionsAfter(cutoffTime: DateTime): Unit =
    while (start < executions.length)
      if (executions(start).executionTime > cutoffTime)
        start += 1
      else
        return

  /**
   * @return the most recent block of passes or fails from the executions which haven't yet been filtered-out, or None
   *   if there are no executions left.
   */
  def getMostRecentPassFailBlock: Option[PassFailBlock] =
    if (start >= executions.length)
      None
    else {
      var pos = start
      val newestExecution = executions(pos)
      val passFail = executions(pos).passed
      while (pos < executions.length && executions(pos).passed == passFail)
        pos += 1
      val finish = pos - 1
      val oldestExecution = executions(finish)
      val consecutiveCount = finish - start + 1
      val interval = oldestExecution.executionTime to newestExecution.executionTime
      Some(PassFailBlock(consecutiveCount, passFail, interval.duration))
    }

}