package com.thetestpeople.trt.utils.http

import java.io.File
import java.net.URI
import java.net.URL

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

object PathCachingHttp {

  def relativePath(urlString: String): String = {
    val url = new URL(urlString)
    val host = url.getHost
    val path = url.getPath
    val chunks = host +: path.split("/").toSeq :+ DigestUtils.md5Hex(urlString)
    chunks.mkString("/")
  }

}

/**
 * Useful for testing offline -- caches URLs on the filesystem
 */
class PathCachingHttp(delegate: Http) extends Http {

  private val cacheRoot = new File("./webcache")
  cacheRoot.mkdirs()

  private def cacheFile(urlString: String): File =
    new File(cacheRoot, PathCachingHttp.relativePath(urlString))

  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse = {
    val file = cacheFile(url.toString)
    if (!file.exists)
      FileUtils.write(file, delegate.get(url).body)
    HttpResponse(200, "OK", FileUtils.readFileToString(file))
  }

  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse =
    throw new HttpException

}
