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

  def ordinalName(n: Int): String = n match {
    case 11 | 12 | 13 ⇒ n + "th"
    case _ ⇒ (n % 10) match {
      case 1 ⇒ n + "st"
      case 2 ⇒ n + "nd"
      case 3 ⇒ n + "rd"
      case _ ⇒ n + "th"
    }
  }

  def printStackTrace(t: Throwable): String = {
    val stringWriter = new StringWriter
    t.printStackTrace(new PrintWriter(stringWriter))
    stringWriter.toString
  }

}