package com.thetestpeople.trt.jenkins.trigger

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model.QualifiedName

@RunWith(classOf[JUnitRunner])
class MavenParamHelperTest extends FlatSpec with ShouldMatchers {

  "A single test from a single class" should "work" in {
    mavenTestNames(QualifiedName("testMethod", "com.example.Test")) should equal(
      "com.example.Test#testMethod")
  }

  "Multiple tests from a single class" should "work" in {
    mavenTestNames(
      QualifiedName("testMethod1", "com.example.Test"),
      QualifiedName("testMethod2", "com.example.Test")) should equal(
        "com.example.Test#testMethod1+testMethod2")
  }

  "Tests from multiple classes" should "work" in {
    mavenTestNames(
      QualifiedName("testMethod1", "com.example.Test1"),
      QualifiedName("testMethod2", "com.example.Test2")) should equal(
        "com.example.Test1#testMethod1,com.example.Test2#testMethod2")
  }

  private def mavenTestNames(testNames: QualifiedName*): String =
    MavenParamHelper.mavenTestNames(testNames.toList)

}