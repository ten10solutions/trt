package com.thetestpeople.trt.jenkins.trigger

import com.thetestpeople.trt.model.jenkins.JenkinsJobParam
import com.thetestpeople.trt.model.QualifiedName
import com.thetestpeople.trt.model.Test

object ParameterSubstitutor {

  def constructParameters(params: List[JenkinsJobParam], tests: List[Test]): List[BuildParameter] = {
    val names = tests.map(_.qualifiedName)
    params.map(p ⇒ BuildParameter(p.param, substitute(p.value, names)))
  }

  private def substitute(template: String, names: List[QualifiedName]): String =
    template
      .replace("$" + MavenParamHelper.VariableName, MavenParamHelper.mavenTestNames(names))
      .replace("$" + SSTParamHelper.VariableName, SSTParamHelper.sstRegexes(names))

}