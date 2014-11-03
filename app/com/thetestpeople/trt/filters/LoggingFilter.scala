package com.thetestpeople.trt.filters

import play.api.Logger
import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import com.thetestpeople.trt.utils.HasLogger

object LoggingFilter extends Filter with HasLogger {

  def apply(nextFilter: (RequestHeader) ⇒ Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    nextFilter(requestHeader).map { result ⇒
      val duration = System.currentTimeMillis - startTime
      if (!requestHeader.uri.startsWith("/assets/"))
        logger.debug(s"[${duration}ms] ${requestHeader.method} ${requestHeader.uri} => ${result.header.status}")
      result
    }
  }

}