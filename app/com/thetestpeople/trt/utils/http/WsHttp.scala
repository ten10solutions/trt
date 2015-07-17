package com.thetestpeople.trt.utils.http

import java.net.URI
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import com.ning.http.client.Realm.AuthScheme
import play.api.libs.ws.WSResponse
import play.api.libs.ws.WS._
import play.api.libs.ws._
import play.api.Play.current
import com.thetestpeople.trt.utils.HasLogger

class WsHttp(client: WSClient = WS.client, timeout: Duration = 60.seconds) extends Http with HasLogger {

  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse =
    try {
      logger.debug(curlGet(url, basicAuthOpt))
      val future = client.url(url.toString).withBasicAuth(basicAuthOpt).get()
      val response = await(future)
      response
    } catch {
      case e: Exception ⇒
        throw new HttpException(s"Error with HTTP GET from $url", e)
    }

  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse =
    try {
      logger.debug(curlPost(url, basicAuthOpt, bodyParams))
      val future = client.url(url.toString).withBasicAuth(basicAuthOpt).post(bodyParams)
      val response = await(future)
      response
    } catch {
      case e: Exception ⇒
        throw new HttpException(s"Problem with HTTP POST to $url", e)
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

  private def curlUserClause(c: Credentials) = s"--user '${c.username}:${c.password}'"

  private def curlGet(url: URI, basicAuthOpt: Option[Credentials]): String = {
    val userClause = basicAuthOpt.map(curlUserClause).getOrElse("")
    s"curl -v --globoff -X GET '$url' $userClause"
  }

  private def curlPost(url: URI, basicAuthOpt: Option[Credentials], bodyParams: Map[String, Seq[String]]) = {
    val userClause = basicAuthOpt.map(curlUserClause).getOrElse("")
    val paramClauses =
      for {
        (paramName, paramValues) ← bodyParams.toSeq
        paramValue ← paramValues
      } yield s"--data-urlencode '${paramName}=${paramValue}'"
    s"curl -v --globoff -X POST '$url' $userClause ${paramClauses.mkString(" ")}"
  }

}
