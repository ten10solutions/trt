package com.thetestpeople.trt.utils

import java.util.concurrent.locks.Lock

object LockUtils {

  implicit class RichLock(lock: Lock) {

    def withLock[T](p: ⇒ T): T = {
      lock.lock()
      try
        p
      finally
        lock.unlock()
    }

  }

}