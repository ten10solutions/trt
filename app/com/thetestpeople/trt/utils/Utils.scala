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

  def ordinalName(n: Int): String = {
    val suffix =
      n match {
        case 11 | 12 | 13 ⇒ "th"
        case _ ⇒ (n % 10) match {
          case 1 ⇒ "st"
          case 2 ⇒ "nd"
          case 3 ⇒ "rd"
          case _ ⇒ "th"
        }
      }
    n + suffix
  }

  def printStackTrace(t: Throwable): String = {
    val stringWriter = new StringWriter
    t.printStackTrace(new PrintWriter(stringWriter))
    stringWriter.toString
  }

}