package com.thetestpeople.trt.jenkins.trigger

import com.thetestpeople.trt.model.jenkins.JenkinsJobParam
import com.thetestpeople.trt.model.QualifiedName
import com.thetestpeople.trt.model.Test

object ParameterSubstitutor {

  def constructParameters(params: Seq[JenkinsJobParam], tests: Seq[Test]): Seq[BuildParameter] = {
    val names = tests.map(_.qualifiedName)
    params.map(p ⇒ BuildParameter(p.param, substitute(p.value, names)))
  }

  private def substitute(template: String, names: Seq[QualifiedName]): String =
    template
      .replace("$" + MavenParamHelper.VariableName, MavenParamHelper.mavenTestNames(names))
      .replace("$" + SSTParamHelper.VariableName, SSTParamHelper.sstRegexes(names))
      .replace("$SPACE_SEPARATED_GROUPS", spaceSeparatedGroups(names))

  private def spaceSeparatedGroups(names: Seq[QualifiedName]): String =
    names.flatMap(_.groupOpt).distinct.mkString(" ")
}