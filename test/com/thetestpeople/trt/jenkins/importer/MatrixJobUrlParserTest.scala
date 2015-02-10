package com.thetestpeople.trt.jenkins.importer

import java.net.URISyntaxException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._

import com.thetestpeople.trt.importer.jenkins.AxisValue
import com.thetestpeople.trt.importer.jenkins.MatrixConfiguration
import com.thetestpeople.trt.importer.jenkins.MatrixJobUrlParser
import com.thetestpeople.trt.utils.UriUtils

@RunWith(classOf[JUnitRunner])
class MatrixJobUrlParserTest extends FlatSpec with Matchers {

  "Parsing a matrix job" should "find the axis values" in {
    val url = UriUtils.uri("https://jenkins.example.com/job/foo/Browser_Type=desktop,label=linux/10")

    val Some(actualConfiguration) = MatrixJobUrlParser.getConfigurations(url)
    
    val expectedConfiguration = MatrixConfiguration(Seq(
      AxisValue("Browser_Type", "desktop"), AxisValue("label", "linux")))
    actualConfiguration should equal(expectedConfiguration)
  }

  it should "handle URL escape sequences" in {
    val url = UriUtils.uri("https://jenkins.example.com/job/foo/Browser=Internet%20Explorer/5")

    val Some(actualConfiguration) = MatrixJobUrlParser.getConfigurations(url)
    
    val expectedConfiguration = MatrixConfiguration(Seq(AxisValue("Browser", "Internet Explorer")))
    actualConfiguration should equal(expectedConfiguration)
  }

}