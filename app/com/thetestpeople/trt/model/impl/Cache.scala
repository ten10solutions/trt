package com.thetestpeople.trt.model.impl

import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import com.thetestpeople.trt.utils.LockUtils._

object Cache {

  def apply[T](getItem: ⇒ T) = new Cache(getItem)

  def invalidate[S](caches: Cache[_]*)(p: ⇒ S): S = {
    def inv(caches: List[Cache[_]]): S = caches match {
      case Nil           ⇒ p
      case cache :: rest ⇒ cache.invalidate(inv(rest))
    }
    inv(caches.toList)
  }

}

/**
 * A read-through cache that can be notified when it is invalid.
 *
 * @param getItem -- the action to perform to calculate/retrieve the item on a cache miss
 *
 */
class Cache[T](getItem: ⇒ T) {

  private var itemOpt: Option[T] = None

  private val lock = new ReentrantReadWriteLock

  /**
   * Execute the given action p, which will invalidate the cache.
   */
  def invalidate[S](p: ⇒ S): S = lock.readLock.withLock {
    itemOpt = None
    p
  }

  def get: T = itemOpt getOrElse lock.writeLock.withLock {
    val item = getItem
    itemOpt = Some(item)
    item
  }

}