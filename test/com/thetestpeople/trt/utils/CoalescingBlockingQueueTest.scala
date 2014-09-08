package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._

@RunWith(classOf[JUnitRunner])
class CoalescingBlockingQueueTest extends FlatSpec with Matchers {

  "Duplicate items" should "be ignored" in {

    val queue = new CoalescingBlockingQueue[Int]
    queue.offer(42)
    queue.offer(42)

    queue.take() should be(42)
    queue.isEmpty should be(true)
  }

  "Queue" should "preserve order" in {
    val queue = new CoalescingBlockingQueue[Int]
    queue.offer(1)
    queue.offer(2)
    queue.offer(3)
    drainQueue(queue) should equal(Seq(1, 2, 3))
  }

  it should "Keep the order of the first copy of an item inserted" in {
    val queue = new CoalescingBlockingQueue[Int]
    queue.offer(1)
    queue.offer(2)
    queue.offer(3)
    queue.offer(3)
    queue.offer(2)
    queue.offer(1)
    drainQueue(queue) should equal(Seq(1, 2, 3))
  }

  private def drainQueue[T](queue: CoalescingBlockingQueue[T]): Seq[T] = {
    var items = Seq[T]()
    while (queue.nonEmpty)
      items = items :+ queue.take()
    items
  }
  
}