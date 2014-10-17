package com.thetestpeople.trt.service

import com.thetestpeople.trt.jenkins.importer.FakeCiImportQueue
import com.thetestpeople.trt.utils.http.AlwaysFailingHttp
import com.thetestpeople.trt.model.impl.MockDao
import com.thetestpeople.trt.analysis.AnalysisService
import com.thetestpeople.trt.service.indexing.LuceneLogIndexer
import com.thetestpeople.trt.jenkins.importer.CiImportStatusManager
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.jenkins.importer.CiImporter
import com.thetestpeople.trt.model.Dao

object TestServiceFactory {

  def setup(http: Http = AlwaysFailingHttp, clock: Clock = FakeClock()) = {
    val dao = new MockDao
    val analysisService = new AnalysisService(dao, clock, async = false)
    val logIndexer = LuceneLogIndexer.memoryBackedIndexer
    val batchRecorder = new BatchRecorder(dao, clock, analysisService, logIndexer)
    val ciImportStatusManager = new CiImportStatusManager(clock)
    val ciImporter = new CiImporter(clock, http, dao, ciImportStatusManager, batchRecorder)
    val service = new ServiceImpl(dao, clock, http, analysisService, ciImportStatusManager, batchRecorder, FakeCiImportQueue, logIndexer)
    ServiceBundle(service, batchRecorder, ciImporter, analysisService, dao)
  }

}

case class ServiceBundle(service: Service, batchRecorder: BatchRecorder, ciImporter: CiImporter, analysisService: AnalysisService, dao: Dao) {
  
}
