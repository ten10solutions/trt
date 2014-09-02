package com.thetestpeople.trt.utils

import scala.xml._

object TestUtils {

  def loadXmlFromClasspath(filename: String): Elem =
    getClass.getResourceAsStream(filename) match {
      case null   ⇒ throw new RuntimeException(s"Could not load '$filename' from classpath")
      case stream ⇒ XML.load(stream)
    }

}