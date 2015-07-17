package com.thetestpeople.trt.api

import java.net.URI

import org.scalatest.FlatSpec
import org.scalatest.Matchers

import com.thetestpeople.trt.FakeApplicationFactory
import com.thetestpeople.trt.json.RestApi

import play.api.test.Helpers
import play.api.test.TestServer

abstract class AbstractApiTest extends FlatSpec with Matchers {

  private val port = 9001

  private val siteUrl = new URI("http://localhost:" + port)

  protected def withApi[T](p: RestApi ⇒ T): T = Helpers.running(TestServer(port, FakeApplicationFactory.fakeApplication)) {
    val restApi = new RestApi(siteUrl)
    restApi.deleteAll()
    p(restApi)
  }

}