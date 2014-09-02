package com.thetestpeople.trt.jenkins.trigger

import java.net.URI
import org.junit.runner.RunWith
import org.scalatest._
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http._
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class JenkinsBuildInvokerTest extends FlatSpec with ShouldMatchers {

  "Build invoker" should "should allow authentication with a user's API token" in {
    val http = new PostCapturingHttp
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl, credentialsOpt = Some(DummyData.JenkinsCredentials))

    jenkinsBuildInvoker.triggerBuild()

    val List(post) = http.posts
    post.url should equal(DummyData.JobUrl / "build")
    post.basicAuthOpt should equal(Some(DummyData.JenkinsCredentials))
    post.bodyParams.get("cause") should be(None)
  }

  it should "allow authentication using a build token" in {
    val http = new PostCapturingHttp
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl, authenticationTokenOpt = Some(DummyData.JenkinsToken))

    jenkinsBuildInvoker.triggerBuild()

    val List(post) = http.posts
    val List(token) = post.bodyParams("token")
    token should equal(DummyData.JenkinsToken)
    post.bodyParams.get("cause").isDefined should be(true)
  }

  it should "should pass the crumb CSRF field" in {
    val http = new PostCapturingHttp
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl)

    jenkinsBuildInvoker.triggerBuild(crumbOpt = Some(DummyData.JenkinsCrumb))

    val List(post) = http.posts
    post.bodyParams(DummyData.JenkinsCrumb.crumbRequestField) should equal(Seq(DummyData.JenkinsCrumb.crumb))
  }

  it should "POST multiple parameters" in {
    val http = new PostCapturingHttp
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl)
    val buildParameters = List(
      BuildParameter("paramName1", "paramValue1"),
      BuildParameter("paramName2", "paramValue2"))

    jenkinsBuildInvoker.triggerBuild(buildParameters)

    val List(post) = http.posts
    val List(jsonParam) = post.bodyParams("json")
    Json.parse(jsonParam) should equal(Json.parse("""
         {
           "parameter": [
             {"name": "paramName1", "value": "paramValue1"},
             {"name": "paramName2", "value": "paramValue2"}
           ]
         }
    """))
  }

  it should "recognise a 201 Created response as success" in {
    val http = onPost(createdResponse())
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl)

    val TriggerResult.Success(DummyData.JobUrl) = jenkinsBuildInvoker.triggerBuild(List(), None)
  }

  it should "recognise an authentication problem" in {
    val http = onPost(CommonHttpResponses.badCredentialsResponse())
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl)

    val TriggerResult.AuthenticationProblem(_) = jenkinsBuildInvoker.triggerBuild(List(), None)
  }

  it should "recognise an authorisation problem" in {
    val http = onPost(CommonHttpResponses.forbiddenResponse())
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl)

    val TriggerResult.AuthenticationProblem(_) = jenkinsBuildInvoker.triggerBuild(List(), None)
  }

  it should "recognise a bad parameter" in {
    val http = onPost(errorNoSuchParamResponse("dodgyParam"))
    val jenkinsBuildInvoker = new JenkinsBuildInvoker(http, jobUrl = DummyData.JobUrl)

    val TriggerResult.ParameterProblem(param) = jenkinsBuildInvoker.triggerBuild(List(), None)
    param should equal("dodgyParam")
  }

  private def onPost(response: HttpResponse): Http = new AbstractHttp {
    override def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse = response
  }

  private case class HttpPost(url: URI, basicAuthOpt: Option[Credentials], bodyParams: Map[String, Seq[String]])

  private class PostCapturingHttp extends AbstractHttp {

    var posts = Seq[HttpPost]()

    override def post(url: URI, basicAuthOpt: Option[Credentials] = None, bodyParams: Map[String, Seq[String]]): HttpResponse = {
      posts = posts :+ HttpPost(url, basicAuthOpt, bodyParams)
      createdResponse()
    }

  }

  private def createdResponse() = HttpResponse(status = 201, statusText = "Created")

  private def errorNoSuchParamResponse(param: String) = HttpResponse(
    status = 500,
    statusText = "Server Error",
    body = s"""...snipped...Caused by: java.lang.IllegalArgumentException: No such parameter definition: $param
    at hudson.model.ParametersDefinitionProperty._doBuild(ParametersDefinitionProperty.java:137)
    at hudson.model.AbstractProject.doBuild(AbstractProject.java:1852)
    at hudson.model.AbstractProject.doBuild(AbstractProject.java:1869)
    at sun.reflect.GeneratedMethodAccessor184.invoke(Unknown Source)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
    at java.lang.reflect.Method.invoke(Method.java:606)
    at org.kohsuke.stapler.Function$$InstanceFunction.invoke(Function.java:298)
    at org.kohsuke.stapler.Function.bindAndInvoke(Function.java:161)
    at org.kohsuke.stapler.Function.bindAndInvokeAndServeResponse(Function.java:96)
    at org.kohsuke.stapler.MetaClass$$1.doDispatch(MetaClass.java:120)
    at org.kohsuke.stapler.NameBasedDispatcher.dispatch(NameBasedDispatcher.java:53)
    at org.kohsuke.stapler.Stapler.tryInvoke(Stapler.java:728)
    ... 63 more
    ...snipped...
""")

}