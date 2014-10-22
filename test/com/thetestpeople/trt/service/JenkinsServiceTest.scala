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
import com.thetestpeople.trt.importer._
import com.thetestpeople.trt.jenkins.importer.FakeCiImportQueue
import com.thetestpeople.trt.service.indexing.LuceneLogIndexer

@RunWith(classOf[JUnitRunner])
class CiServiceTest extends FlatSpec with ShouldMatchers {

  "Creating a Jenkins import spec" should "not persist lastCheckedOpt" in {
    val service = setup().service
    val spec = F.ciImportSpec(lastCheckedOpt = Some(5.minutes.ago))
    val specId = service.newCiImportSpec(spec)
    val Some(specAgain) = service.getCiImportSpec(specId)
    specAgain.lastCheckedOpt should equal(None)

    // Everything should be persisted, except lastCheckedOpt (and Id)
    specAgain.copy(id = spec.id, lastCheckedOpt = spec.lastCheckedOpt) should equal(spec)
  }

  private def setup() = TestServiceFactory.setup()

}
