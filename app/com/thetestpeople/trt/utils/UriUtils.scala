package com.thetestpeople.trt.utils

import java.net.URI
import java.net.URL
import java.net.URISyntaxException
import org.apache.http.client.utils.URIBuilder

object UriUtils {

  implicit class RichUri(uri: URI) {

    def /(s: String): URI = new URI(uri.toString + "/" + s).normalize

    def ?(s: String): URI = new URI(uri.toString + "?" + s)

    def withSameHostAndPortAs(other: URI): URI =
      new URIBuilder(uri).setHost(other.getHost).setPort(other.getPort).build

  }

  @throws[URISyntaxException]
  def uri(s: String) = new URI(s)

}