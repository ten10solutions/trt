package com.thetestpeople.trt.utils.http

import java.net.URI

import com.thetestpeople.trt.utils.http._
import com.google.common.base.Charsets
import com.google.common.io.Resources

class ClasspathCachingHttp(var prefix: String) extends Http {

  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse = {
    val body = fetchString(url.toString)
    HttpResponse(200, "OK", body)
  }

  private def fetchString(url: String): String =
    try {
      val path = prefix + "/" + PathCachingHttp.relativePath(url)
      Resources.toString(Resources.getResource(path), Charsets.UTF_8);
    } catch {
      case e: Exception ⇒ throw new HttpException(s"Problem fetching $url", e)
    }

  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse =
    throw new HttpException

}