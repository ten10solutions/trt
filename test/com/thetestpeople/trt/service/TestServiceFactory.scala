package com.thetestpeople.trt.service

import com.thetestpeople.trt.jenkins.importer.FakeJenkinsImportQueue
import com.thetestpeople.trt.utils.http.AlwaysFailingHttp
import com.thetestpeople.trt.model.impl.MockDao
import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.service.indexing.LuceneLogIndexer
import com.thetestpeople.trt.jenkins.importer.JenkinsImportStatusManager
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.jenkins.importer.JenkinsImporter
import com.thetestpeople.trt.model.Dao

object TestServiceFactory {

  def setup(http: Http = AlwaysFailingHttp, clock: Clock = FakeClock()) = {
    val dao = new MockDao
    val analysisService = new AnalysisService(dao, clock, async = false)
    val logIndexer = LuceneLogIndexer.memoryBackedIndexer
    val batchRecorder = new BatchRecorder(dao, clock, analysisService, logIndexer)
    val jenkinsImportStatusManager = new JenkinsImportStatusManager(clock)
    val jenkinsImporter = new JenkinsImporter(clock, http, dao, jenkinsImportStatusManager, batchRecorder)
    val service = new ServiceImpl(dao, clock, http, analysisService, jenkinsImportStatusManager, batchRecorder, FakeJenkinsImportQueue, logIndexer)
    ServiceBundle(service, batchRecorder, jenkinsImporter, analysisService, dao)
  }

}

case class ServiceBundle(service: Service, batchRecorder: BatchRecorder, jenkinsImporter: JenkinsImporter, analysisService: AnalysisService, dao: Dao) {
  
}
