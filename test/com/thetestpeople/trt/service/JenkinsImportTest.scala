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
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.utils.http.AlwaysFailingHttp
import com.thetestpeople.trt.utils.http.ClasspathCachingHttp
import com.thetestpeople.trt.importer._
import com.thetestpeople.trt.importer.CiImporter
import com.thetestpeople.trt.service.indexing.LuceneLogIndexer

@RunWith(classOf[JUnitRunner])
class JenkinsImportTest extends FlatSpec with ShouldMatchers {

  // TODO: need to capture some more data to run this test with new API usage
  "Importing from jenkins" should "work" ignore {

    val clock = FakeClock()
    val http = new ClasspathCachingHttp("")
    val serviceBundle = setup(http, clock)
    val service = serviceBundle.service
    val ciImporter = serviceBundle.ciImporter

    val specId = service.newCiImportSpec(F.ciImportSpec(
      jobUrl = new URI("http://ci.pentaho.com/job/pentaho-big-data-plugin/"),
      pollingInterval = 2.minutes,
      importConsoleLog = false))

    http.prefix = "webcache-pentaho-1214-1216"

    service.syncAllCiImports()
    ciImporter.importBuilds(specId)

    service.getBatches().flatMap(_.nameOpt) should equal(List(
      "pentaho-big-data-plugin #1216",
      "pentaho-big-data-plugin #1215",
      "pentaho-big-data-plugin #1214",
      "pentaho-big-data-plugin #57"))

    http.prefix = "webcache-pentaho-1217-1219"
    clock += 10.minutes

    service.syncAllCiImports()
    ciImporter.importBuilds(specId)

    service.getBatches().flatMap(_.nameOpt) should equal(List(
      "pentaho-big-data-plugin #1219",
      "pentaho-big-data-plugin #1218",
      "pentaho-big-data-plugin #1217",
      "pentaho-big-data-plugin #1216",
      "pentaho-big-data-plugin #1215",
      "pentaho-big-data-plugin #1214",
      "pentaho-big-data-plugin #57"))

  }

  private def setup(http: Http = AlwaysFailingHttp, clock: Clock = FakeClock()) = 
    TestServiceFactory.setup(http = http, clock = clock)
}