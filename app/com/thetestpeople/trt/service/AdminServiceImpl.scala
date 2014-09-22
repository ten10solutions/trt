package com.thetestpeople.trt.service

import com.thetestpeople.trt.model.Dao
import com.thetestpeople.trt.model.DaoAdmin
import play.api.Logger
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.service.indexing.LogIndexer

class AdminServiceImpl(daoAdmin: DaoAdmin, logIndexer: LogIndexer) extends AdminService with HasLogger {

  def deleteAll() {
    logger.info("Deleted all data")
    daoAdmin.deleteAll()
    logIndexer.deleteAll()
  }

}