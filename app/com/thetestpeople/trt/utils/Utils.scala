package com.thetestpeople.trt.utils

import play.api.Logger
import java.io.StringWriter
import java.io.PrintWriter

object Utils extends HasLogger {

  def time[T](label: String)(p: ⇒ T) = {
    val start = System.nanoTime
    try
      p
    finally {
      val duration = (System.nanoTime - start) / 1000000.0
      logger.debug(s"$label: $duration ms")

    }
  }

  def ordinalName(n: Int) = StringUtils.ordinalName(n) // TODO: replace
  
  def printStackTrace(t: Throwable): String = {
    val stringWriter = new StringWriter
    t.printStackTrace(new PrintWriter(stringWriter))
    stringWriter.toString
  }

}