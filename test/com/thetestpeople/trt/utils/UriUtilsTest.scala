package com.thetestpeople.trt.utils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.thetestpeople.trt.utils.UriUtils._

@RunWith(classOf[JUnitRunner])
class UriUtilsTest extends FlatSpec with Matchers {

  "It" should "let you construct sub-URLs  with a slash" in {

    uri("http://www.example.com/") / "jenkins" should equal(uri("http://www.example.com/jenkins"))
    uri("http://www.example.com") / "jenkins" should equal(uri("http://www.example.com/jenkins"))

    uri("http://localhost:8080/job/Rerun%20Test%20Job") / "api/xml" should equal(uri("http://localhost:8080/job/Rerun%20Test%20Job/api/xml"))
    uri("http://localhost:8080/job/Rerun%20Test%20Job/") / "api/xml" should equal(uri("http://localhost:8080/job/Rerun%20Test%20Job/api/xml"))
  }

}