package com.thetestpeople.trt.utils.http

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import com.ning.http.client.Realm.AuthScheme
import play.api.libs.ws.Response
import play.api.libs.ws.WS._
import play.api.libs.ws._
import play.api.Play.current
import com.thetestpeople.trt.utils.HasLogger

class WsHttp(timeout: Duration = 60.seconds) extends Http with HasLogger {

  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse =
    try {
      val future = WS.url(url.toString).withBasicAuth(basicAuthOpt).get()
      val response = await(future)
      logger.debug("GET: " + url.toString)
      logger.debug(curlGet(url, basicAuthOpt))
      logger.debug("GET response: " + response.statusText)
      response
    } catch {
      case e: Exception ⇒ throw new HttpException(s"Error with HTTP GET from $url", e)
    }

  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse =
    try {
      val future = WS.url(url.toString).withBasicAuth(basicAuthOpt).post(bodyParams)
      val response = await(future)
      logger.debug("POST to: " + url.toString)
      logger.debug(curlPost(url, basicAuthOpt, bodyParams))
      logger.debug("POST response: " + response.statusText)
      response
    } catch {
      case e: Exception ⇒ throw new HttpException(s"Problem with HTTP POST to $url", e)
    }

  private def await(future: Future[WSResponse]): HttpResponse = {
    val response = Await.result(future, timeout)
    HttpResponse(response.status, response.statusText, response.body)
  }

  private implicit class EnhancedRequestHolder(request: WSRequestHolder) {
    def withBasicAuth(credentialsOpt: Option[Credentials]): WSRequestHolder = credentialsOpt match {
      case Some(Credentials(username, apiToken)) ⇒ request.withAuth(username, apiToken, WSAuthScheme.BASIC)
      case None                                  ⇒ request
    }
  }

  private def curlUserClause(c: Credentials) = s"--user ${c.username}:${c.password}"

  private def curlGet(url: URI, basicAuthOpt: Option[Credentials]): String = {
    val userClause = basicAuthOpt.map(curlUserClause).getOrElse("")
    s"curl -v -X GET ${url.toString} $userClause"
  }

  private def curlPost(url: URI, basicAuthOpt: Option[Credentials], bodyParams: Map[String, Seq[String]]) = {
    val userClause = basicAuthOpt.map(curlUserClause).getOrElse("")
    val paramClauses =
      for {
        (paramName, paramValues) ← bodyParams.toList
        paramValue ← paramValues
      } yield s"--data-urlencode '${paramName}=${paramValue}'"
    s"curl -v -X POST $url --user trt:8c31d02432cdea46547723875b0d7cf9 ${paramClauses.mkString(" ")}"
  }

}
