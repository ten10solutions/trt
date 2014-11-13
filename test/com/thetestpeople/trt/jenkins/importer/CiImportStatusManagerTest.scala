package com.thetestpeople.trt.jenkins.importer

import org.joda.time.Duration
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.utils.TestUtils
import com.thetestpeople.trt.utils.UriUtils._
import java.net.URI
import com.thetestpeople.trt.service.FakeClock
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins.CiImportSpec
import com.thetestpeople.trt.model.impl.DummyData
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.importer._

@RunWith(classOf[JUnitRunner])
class CiImportStatusManagerTest extends FlatSpec with Matchers {

  "The status manager" should "let you start and complete a job" in {
    val clock = FakeClock()
    val statusManager = new CiImportStatusManager(clock)

    val specId = Id[CiImportSpec](1)
    statusManager.importStarted(specId, DummyData.JobUrl)

    val Some(jobStatus1) = statusManager.getJobImportStatus(specId)
    jobStatus1.updatedAt should equal(clock.now)
    jobStatus1.state should equal(JobImportState.InProgress)

    clock += 5.minutes
    statusManager.importComplete(specId)

    val Some(jobStatus2) = statusManager.getJobImportStatus(specId)
    jobStatus2.updatedAt should equal(clock.now)
    jobStatus2.state should equal(JobImportState.Complete)
  }

  it should "record job import errors" in {
    val clock = FakeClock()
    val statusManager = new CiImportStatusManager(clock)
    val specId = Id[CiImportSpec](1)
    statusManager.importStarted(specId, DummyData.JobUrl)
    clock += 5.minutes

    val exception = new RuntimeException("some exception")
    statusManager.importErrored(specId, exception)

    val Some(jobStatus) = statusManager.getJobImportStatus(specId)
    jobStatus.updatedAt should equal(clock.now)
    val JobImportState.Errored(exceptionAgain) = jobStatus.state
    exceptionAgain should equal(exception)
  }

  it should "record build imports" in {
    val clock = FakeClock()
    val statusManager = new CiImportStatusManager(clock)
    val specId = Id[CiImportSpec](1)
    statusManager.importStarted(specId, DummyData.JobUrl)

    statusManager.buildExists(specId, DummyData.BuildUrl, Some(DummyData.BuildNumber))

    val Seq(buildStatus1) = statusManager.getBuildImportStatuses(specId)
    buildStatus1.buildNumberOpt should equal(Some(DummyData.BuildNumber))
    buildStatus1.buildUrl should equal(DummyData.BuildUrl)
    buildStatus1.updatedAt should equal(clock.now)
    buildStatus1.state should equal(BuildImportState.NotStarted)

    statusManager.buildStarted(specId, DummyData.BuildUrl)
    val Seq(buildStatus2) = statusManager.getBuildImportStatuses(specId)
    buildStatus2.updatedAt should equal(clock.now)
    buildStatus2.state should equal(BuildImportState.InProgress)

    val batchId = Id[Batch](1)
    statusManager.buildComplete(specId, DummyData.BuildUrl, Some(batchId))
    val Seq(buildStatus3) = statusManager.getBuildImportStatuses(specId)
    buildStatus3.updatedAt should equal(clock.now)
    val BuildImportState.Complete(Some(batchIdAgain)) = buildStatus3.state
    batchIdAgain should equal(batchId)
  }

}