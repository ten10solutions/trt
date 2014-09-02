package com.thetestpeople.trt.service

import com.thetestpeople.trt.model.Dao
import com.thetestpeople.trt.model.DaoAdmin
import play.api.Logger
import com.thetestpeople.trt.utils.HasLogger

class AdminServiceImpl(daoAdmin: DaoAdmin) extends AdminService with HasLogger {

  def deleteAll() {
    logger.info("Deleted all data")
    daoAdmin.deleteAll()
  }

}