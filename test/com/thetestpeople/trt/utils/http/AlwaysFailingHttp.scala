package com.thetestpeople.trt.utils.http

import java.net.URI

object AlwaysFailingHttp extends Http {

  @throws[HttpException]
  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse = throw new HttpException

  @throws[HttpException]
  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse =
    throw new HttpException

}