package com.thetestpeople.trt.service

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest._
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.utils._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.model.impl.MockDao
import java.net.URI
import com.thetestpeople.trt.utils.http.AlwaysFailingHttp
import com.thetestpeople.trt.utils.http.Http

@RunWith(classOf[JUnitRunner])
class JenkinsServiceTest extends FlatSpec with ShouldMatchers {

  "Creating a Jenkins import spec" should "not persist lastCheckedOpt" in {
    val service = setup().service
    val spec = F.jenkinsImportSpec(lastCheckedOpt = Some(5.minutes.ago))
    val specId = service.newJenkinsImportSpec(spec)
    val Some(specAgain) = service.getJenkinsImportSpec(specId)
    specAgain.lastCheckedOpt should equal(None)

    // Everything should be persisted, except lastCheckedOpt (and Id)
    specAgain.copy(id = spec.id, lastCheckedOpt = spec.lastCheckedOpt) should equal(spec)
  }

  private def setup(http: Http = AlwaysFailingHttp, clock: Clock = FakeClock()) = {
    val dao = new MockDao
    val analysisService = new AnalysisService(dao, clock, async = false)
    val batchRecorder = new BatchRecorder(dao, clock, analysisService)
    val service = new ServiceImpl(dao, clock, http, analysisService)
    Setup(service, batchRecorder)
  }

  case class Setup(service: Service, batchRecorder: BatchRecorder)

}
