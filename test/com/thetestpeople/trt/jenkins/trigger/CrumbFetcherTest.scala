package com.thetestpeople.trt.jenkins.trigger

import java.net.URI
import org.junit.runner.RunWith
import org.scalatest._
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CrumbFetcherTest extends FlatSpec with ShouldMatchers {

  "Crumb fetcher" should "make the correct HTTP GET call" in {
    var requests = Seq[GetRequest]()
    val http = new GetRequestCapturingHttp
    val crumbFetcher = new CrumbFetcher(http, DummyData.JenkinsUrl, credentialsOpt = Some(DummyData.JenkinsCredentials))

    crumbFetcher.getCrumb()

    val expectedUrl = DummyData.JenkinsUrl / "crumbIssuer/api/json"
    http.requests should equal(List(GetRequest(expectedUrl, Some(DummyData.JenkinsCredentials))))
  }

  it should "parse a crumb if one is returned" in {
    val http = onGet(okResponse(DummyData.JenkinsCrumb))
    val crumbFetcher = new CrumbFetcher(http, DummyData.JenkinsUrl)

    val CrumbFetchResult.Found(crumb) = crumbFetcher.getCrumb()

    crumb should equal(DummyData.JenkinsCrumb)
  }

  it should "recognise an authentication problem" in {
    val http = onGet(CommonHttpResponses.badCredentialsResponse())
    val crumbFetcher = new CrumbFetcher(http, DummyData.JenkinsUrl)

    val CrumbFetchResult.AuthenticationProblem(_) = crumbFetcher.getCrumb()
  }

  it should "recognise an authorisation problem" in {
    val http = onGet(CommonHttpResponses.forbiddenResponse())
    val crumbFetcher = new CrumbFetcher(http, DummyData.JenkinsUrl)

    val CrumbFetchResult.AuthenticationProblem(_) = crumbFetcher.getCrumb()
  }

  it should "recognise crumb not found" in {
    val http = onGet(notFoundResponse)
    val crumbFetcher = new CrumbFetcher(http, DummyData.JenkinsUrl)

    val result = crumbFetcher.getCrumb()

    result should equal(CrumbFetchResult.NotFound)
  }

  private case class GetRequest(url: URI, basicAuthOpt: Option[Credentials])

  private class GetRequestCapturingHttp extends AbstractHttp {

    var requests = Seq[GetRequest]()

    override def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse = {
      requests = requests :+ GetRequest(url, basicAuthOpt)
      okResponse(DummyData.JenkinsCrumb)
    }

  }

  private def onGet(response: HttpResponse): Http = new AbstractHttp {
    override def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse = response
  }

  private def okResponse(crumb: Crumb) = HttpResponse(
    status = 200,
    statusText = "OK",
    body = s"""{"crumb":"${crumb.crumb}","crumbRequestField":"${crumb.crumbRequestField}"}""")

  private def notFoundResponse() = HttpResponse(
    status = 404,
    statusText = "Not Found",
    body = """<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
<title>Error 404 Not Found</title>
</head>
<body><h2>HTTP ERROR 404</h2>
<p>Problem accessing /crumbIssuer/api/json. Reason:
<pre>    Not Found</pre></p><hr /><i><small>Powered by Jetty://</small></i><br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                
<br/>                                                

</body>
</html>""")

}

abstract class AbstractHttp extends Http {

  def get(url: URI, basicAuthOpt: Option[Credentials] = None): HttpResponse =
    throw new HttpException

  def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse =
    throw new HttpException

}
