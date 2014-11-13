package com.thetestpeople.trt.utils

import java.net.URI
import java.net.URL
import java.net.URISyntaxException

object UriUtils {

  implicit class RichUri(uri: URI) {

    def /(s: String): URI = new URI(uri.toString + "/" + s).normalize

    def ?(s: String): URI = new URI(uri.toString + "?" + s)

  }

  @throws[URISyntaxException]
  def uri(s: String) = new URI(s)

}