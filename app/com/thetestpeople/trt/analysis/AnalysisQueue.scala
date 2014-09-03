package com.thetestpeople.trt.analysis

import java.util.concurrent.BlockingQueue
import com.thetestpeople.trt.model.Id
import java.util.concurrent.LinkedBlockingQueue
import com.thetestpeople.trt.model.Test

/**
 * Queue of tests which need their analysis updating. Only a single instance of each test ID is kept on the queue at
 * one time.
 */
class AnalysisQueue {

  private val testQueue: BlockingQueue[Id[Test]] = new LinkedBlockingQueue

  private var testSet: Set[Id[Test]] = Set()

  def offer(testId: Id[Test]) = synchronized {
    if (!testSet.contains(testId)) {
      testQueue.offer(testId)
      testSet = testSet + testId
    }
  }

  def take(): Id[Test] = {
    val testId = testQueue.take()
    synchronized {
      testSet = testSet - testId
    }
    testId
  }

  def size = testQueue.size

  override def toString = synchronized { "AnalysisQueue(" + testSet.map(_.value).toList.sorted + ")" }
  
}