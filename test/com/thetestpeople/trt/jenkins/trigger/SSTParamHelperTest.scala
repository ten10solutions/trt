package com.thetestpeople.trt.jenkins.trigger

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model.QualifiedName

@RunWith(classOf[JUnitRunner])
class SSTParamHelperTest extends FlatSpec with ShouldMatchers {

  "A single test from a single class" should "work" in {
    sstRegexes(QualifiedName("testMethod", "com.example.Test")) should equal(
      """^com\.example\.Test\.testMethod$""")
  }

  private def sstRegexes(testNames: QualifiedName*): String =
    SSTParamHelper.sstRegexes(testNames.toList)

}