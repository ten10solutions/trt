package com.thetestpeople.trt

import org.junit.runner._
import play.api.test._
import play.api.test.Helpers._
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.Config._
import com.thetestpeople.trt.tags.SlowTest

@SlowTest
@RunWith(classOf[JUnitRunner])
class ApplicationTest extends FlatSpec with Matchers {

  "Application" should "send 404 on a bad request" in {
    Helpers.running(FakeApplicationFactory.fakeApplication) {
      route(FakeRequest(GET, "/boom")) should equal(None)
    }
  }

  it should "render the tests page" in {
    Helpers.running(FakeApplicationFactory.fakeApplication) {
      val home = route(FakeRequest(GET, "/tests")).get

      status(home) should be(OK)
      contentType(home) should be(Some("text/html"))
      contentAsString(home) should include("Tests")
    }
  }

}
