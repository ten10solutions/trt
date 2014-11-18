package com.thetestpeople.trt.webdriver.screens

class WaitedTooLongException(message: String = null) extends RuntimeException(message)

object WaitUtils {

  def waitUntil[T](timeout: Int = 30, retryWait: Int = 3)(p: ⇒ Boolean) {
    val start = System.currentTimeMillis
    while (!p) {
      if (System.currentTimeMillis - start > timeout * 1000)
        throw new WaitedTooLongException()
      Thread.sleep(retryWait * 1000)
    }
  }

}