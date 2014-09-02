package com.thetestpeople.trt.jenkins.trigger

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.http._
import java.net.URI

class JenkinsTestRunner(dao: Dao, http: Http) extends HasLogger {

  def rerunTests(testIds: List[Id[Test]]): TriggerResult = {
    val FullJenkinsConfiguration(jenkinsConfig, params) = dao.getJenkinsConfiguration
    val tests = dao.getTestsById(testIds)
    val parameters = ParameterSubstitutor.constructParameters(params, tests)
    val jobUrl = jenkinsConfig.rerunJobUrlOpt.getOrElse(
      throw new RuntimeException("No rerun job has been configured"))

    def invokeBuild(crumbOpt: Option[Crumb]) = {
      val buildInvoker = new JenkinsBuildInvoker(http, jobUrl = jobUrl,
        authenticationTokenOpt = jenkinsConfig.authenticationTokenOpt, credentialsOpt = jenkinsConfig.credentialsOpt)
      val result = buildInvoker.triggerBuild(parameters, crumbOpt)
      if (result.successful)
        logger.info(s"Triggered execution of ${testIds.size} tests on ${jenkinsConfig.rerunJobUrlOpt.get}")
      result
    }

    val jenkinsUrl = JenkinsUrlHelper.getServerUrl(jobUrl)
    val crumbFetcher = new CrumbFetcher(http, jenkinsUrl, jenkinsConfig.credentialsOpt)
    crumbFetcher.getCrumb() match {
      case CrumbFetchResult.Found(crumb)                   ⇒ invokeBuild(Some(crumb))
      case CrumbFetchResult.NotFound                       ⇒ invokeBuild(None)
      case CrumbFetchResult.AuthenticationProblem(message) ⇒ TriggerResult.AuthenticationProblem(message)
      case CrumbFetchResult.Other(message)                 ⇒ TriggerResult.OtherProblem(message)
    }
  }

}