package com.thetestpeople.trt.jenkins.trigger

import com.thetestpeople.trt.model.QualifiedName

/**
 * Argument suitable for running via Maven Surefire:
 *
 * mvn test -Dtest=com.example.tests.Test1#testMethod1,com.example.tests.Test2#testMethod3
 *
 * Syntax is only supported by Surefire plugin version >=2.13
 */
object MavenParamHelper {

  val VariableName = "MAVEN_TEST"

  def mavenTestNames(names: Seq[QualifiedName]): String = {
    val clauses =
      for ((groupOpt, namesInGroup) ← names.groupBy(_.groupOpt).toList.sortBy(_._1))
        yield mavenTestNameClause(groupOpt, namesInGroup.sorted.map(_.name))
    clauses.mkString(",")
  }

  private def mavenTestNameClause(groupOpt: Option[String], namesInGroup: Seq[String]): String =
    groupOpt.map(_ + "#").getOrElse("") + namesInGroup.mkString("+")

}