package com.thetestpeople.trt.utils.http

import java.net.URI

/**
 * Facade for HTTP calls
 */
trait Http {

  @throws[HttpException]
  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse

  /**
   * HTTP POST to the given URI.
   * @param bodyParams -- POST body (will send as url-encoded form params)
   */
  @throws[HttpException]
  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse

}
