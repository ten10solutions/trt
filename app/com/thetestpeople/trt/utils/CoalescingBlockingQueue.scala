package com.thetestpeople.trt.utils

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import scala.collection.JavaConverters._

/**
 * A blocking queue that ignores items added to it if they are already enqueued.
 */
class CoalescingBlockingQueue[T] {

  private val queue: BlockingQueue[T] = new LinkedBlockingQueue

  private var set: Set[T] = Set()

  def offer(item: T) = synchronized {
    if (!set.contains(item)) {
      queue.offer(item)
      set = set + item
    }
  }

  def take(): T = {
    val item = queue.take()
    synchronized {
      set = set - item
    }
    item
  }

  def size = set.size

  def isEmpty = set.size == 0

  def nonEmpty = set.size != 0

  override def toString = synchronized {
    val itemList = queue.asScala.mkString(", ")
    s"CoalescingBlockingQueue($itemList)"
  }

}