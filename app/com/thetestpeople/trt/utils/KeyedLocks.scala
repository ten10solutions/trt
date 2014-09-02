package com.thetestpeople.trt.utils

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Lock
import com.google.common.collect.MapMaker
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader

class KeyedLocks[K <: AnyRef] {

  private val locks = CacheBuilder.newBuilder().weakValues().build(new CacheLoader[K, Lock] {
    def load(key: K) = new ReentrantLock
  })

  def withLock[T](key: K)(p: ⇒ T) = {
    val lock = locks.get(key)
    lock.lock()
    try
      p
    finally
      lock.unlock()
  }

  def tryLock[T](key: K)(p: ⇒ T): Option[T] = {
    val lock: Lock = locks.get(key)
    val success = lock.tryLock()
    if (success)
      try
        Some(p)
      finally
        lock.unlock()
    else
      None
  }

}