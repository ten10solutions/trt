package com.thetestpeople.trt.model.impl

import org.joda.time.DateTime
import org.joda.time.Duration
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model.QualifiedName
import java.net.URI
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.jenkins.trigger.Crumb
import com.thetestpeople.trt.utils.http.Credentials

object DummyData {

  val ExecutionTime = 1.day.ago
  val Duration: Duration = 3.seconds
  val Group = "Group"
  val TestName = "testName"
  val QualifiedTestName = QualifiedName(TestName, Group)
  val BatchName = "batchName"
  val Weather = 1.0
  val BuildUrl = uri("http://www.example.com/job/build1")
  val BuildUrl2 = uri("http://www.example.com/job/build2")
  val Log = "Log\nLog\nLog"
  val Summary = "Summary"
  val TotalCount = 10
  val PassCount = 6
  val FailCount = 4
  val ConsecutiveFailures = 3
  val LastExecuted = 1.day.ago
  val LastPassed = 1.day.ago
  val LastFailed = 2.days.ago
  val WhenAnalysed = 3.hours.ago

  val JobUrl = uri("http://www.example.com/jenkins/someJob")
  val JobUrl2 = uri("http://www.example.com/jenkins/someOtherJob")
  val JobName = "Jenkins Job Name"
  val PollingInterval: Duration = 10.minutes
  val LastChecked = 2.days.ago
  val ImportTime = 5.minutes.ago

  val Username = "JenkinsUser"
  val ApiToken = "36b5d0c5ae8a760bc74b0bb094a061b2"
  val JenkinsCredentials = Credentials(Username, ApiToken)
  val AuthenticationToken = "AuthenticationToken"
  val ParamName = "ParamName"
  val ParamValue = "ParamValue"
  val JenkinsCrumb = Crumb("ab133bb9a146966dd7f34d7cacb2ae38")
  val JenkinsUrl = uri("http://www.example.com/")
  val JenkinsToken = "Token"
  val BuildNumber = 1
  
  val Configuration1 = Configuration("Configuration1")
  val Configuration2 = Configuration("Configuration2")
  
  val Comment = "Comment"
}
