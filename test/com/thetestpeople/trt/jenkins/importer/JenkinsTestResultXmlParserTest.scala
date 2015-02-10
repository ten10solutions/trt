package com.thetestpeople.trt.jenkins.importer

import scala.xml.Elem
import org.joda.time.Duration
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import java.net.URI
import com.thetestpeople.trt.importer.jenkins.JenkinsTestResultXmlParser
import com.thetestpeople.trt.importer.jenkins.TestResult
import com.thetestpeople.trt.importer.jenkins.Suite
import com.thetestpeople.trt.importer.jenkins.OrdinaryTestResult
import com.thetestpeople.trt.importer.jenkins.MatrixTestResult
import com.thetestpeople.trt.model.impl.DummyData

@RunWith(classOf[JUnitRunner])
class JenkinsTestResultXmlParserTest extends FlatSpec with Matchers {

  "Parsing the Jenkins test result API" should "correctly capture the input data" in {

    val xml =
      <testResult>
        <duration>4336.8516</duration>
        <empty>false</empty>
        <failCount>0</failCount>
        <passCount>4416</passCount>
        <skipCount>4</skipCount>
        <suite>
          <case>
            <age>0</age>
            <className>hudson.cli.ConnectionMockTest</className>
            <duration>0.195</duration>
            <failedSince>0</failedSince>
            <name>shouldTolerateEmptyByteArrayUponStreamZeroValue</name>
            <skipped>false</skipped>
            <status>PASSED</status>
          </case>
          <duration>0.195</duration>
          <name>hudson.cli.ConnectionMockTest</name>
        </suite>
      </testResult>

    val OrdinaryTestResult(duration, List(suite), None) = parse(xml)
    duration should be(Duration.millis(4336852))

    suite.name should be("hudson.cli.ConnectionMockTest")
    suite.duration should be(Duration.millis(195))

    val List(testCase) = suite.cases
    testCase.duration should be(Duration.millis(195))
    testCase.status should be("PASSED")
    testCase.passed should be(true)
    testCase.skipped should be(false)
    testCase.name should be("shouldTolerateEmptyByteArrayUponStreamZeroValue")
    testCase.className should be("hudson.cli.ConnectionMockTest")
    testCase.errorDetailsOpt should be(None)
    testCase.errorStackTraceOpt should be(None)
    testCase.stdoutOpt should be(None)
  }

  it should "give the correct status for different types of results" in {

    val xml =
      <testResult>
        <duration>4580.831</duration>
        <empty>false</empty>
        <failCount>1</failCount>
        <passCount>22</passCount>
        <skipCount>5</skipCount>
        <suite>
          <case>
            <age>0</age>
            <className>smoke</className>
            <duration>66.848</duration>
            <failedSince>0</failedSince>
            <name>test1</name>
            <skipped>false</skipped>
            <status>PASSED</status>
          </case>
          <case>
            <age>12</age>
            <className>smoke</className>
            <duration>6.257</duration>
            <failedSince>869</failedSince>
            <name>test2</name>
            <skipped>true</skipped>
            <status>SKIPPED</status>
          </case>
          <case>
            <age>4</age>
            <className>smoke</className>
            <duration>279.289</duration>
            <errorStackTrace>Exception</errorStackTrace>
            <failedSince>877</failedSince>
            <name>test3</name>
            <skipped>false</skipped>
            <status>FAILED</status>
          </case>
          <case>
            <age>0</age>
            <className>smoke</className>
            <duration>717.008</duration>
            <failedSince>0</failedSince>
            <name>test4</name>
            <skipped>false</skipped>
            <status>FIXED</status>
          </case>
          <duration>4580.831</duration>
          <name></name>
        </suite>
      </testResult>

    val OrdinaryTestResult(_, List(Suite(_, _, cases)), None) = parse(xml)
    val Some(passCase) = cases.find(_.name == "test1")
    passCase.passed should be(true)

    val Some(skippedCase) = cases.find(_.name == "test2")
    skippedCase.skipped should be(true)

    val Some(failedCase) = cases.find(_.name == "test3")
    failedCase.errorStackTraceOpt should equal(Some("Exception"))
    failedCase.passed should be(false)

    val Some(fixedCase) = cases.find(_.name == "test4")
    fixedCase.passed should be(true)
  }

  it should "handle matrix test results with a single child result" in {
    val url = new URI("http://server:8080/job/highlevel/Browser=Chrome/870/")
    val xml =
      <matrixTestResult>
        <failCount>1</failCount>
        <skipCount>5</skipCount>
        <totalCount>28</totalCount>
        <urlName>testReport</urlName>
        <childReport>
          <child>
            <number>870</number>
            <url>{ url }</url>
          </child>
          <result>
            <duration>3185.3042</duration>
            <empty>false</empty>
            <failCount>1</failCount>
            <passCount>22</passCount>
            <skipCount>5</skipCount>
            <suite>
              <case>
                <age>0</age>
                <className>smoke</className>
                <duration>37.181</duration>
                <failedSince>0</failedSince>
                <name>test</name>
                <skipped>false</skipped>
                <status>PASSED</status>
              </case>
              <duration>3185.3042</duration>
              <name></name>
            </suite>
          </result>
        </childReport>
      </matrixTestResult>

    val MatrixTestResult(List(actualUrl)) = parse(xml)
    actualUrl should be(url)
  }

  private def parse(xml: Elem): TestResult = new JenkinsTestResultXmlParser().parseTestResult(xml)

}