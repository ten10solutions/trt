package com.thetestpeople.trt.service

import com.thetestpeople.trt.model.Dao
import com.thetestpeople.trt.model.DaoAdmin
import play.api.Logger
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.service.indexing.LogIndexer
import com.thetestpeople.trt.analysis.AnalysisService

class AdminServiceImpl(daoAdmin: DaoAdmin, dao: Dao, logIndexer: LogIndexer, analysisService: AnalysisService) extends AdminService with HasLogger {

  def deleteAll() {
    daoAdmin.deleteAll()
    logIndexer.deleteAll()
    analysisService.deleteAll()
    logger.info("Deleted all data")
  }

  def analyseAll() {
    analysisService.scheduleAnalysis(dao.transaction { dao.getTestIds() } )
  }
  
}