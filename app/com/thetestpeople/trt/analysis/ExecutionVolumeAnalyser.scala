package com.thetestpeople.trt.analysis

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Lock
import com.thetestpeople.trt.utils.LockUtils._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils.HasLogger

class ExecutionVolumeAnalyser(dao: Dao) extends HasLogger {

  private val lock: Lock = new ReentrantLock
  private var analysisResultOpt: Option[ExecutionVolumeAnalysisResult] = None

  def computeExecutionVolumes() =
    lock.withLock {
      val analysisResult = dao.transaction {
        dao.iterateAllExecutions { executions ⇒
          new ExecutionVolumeCalculator().countExecutions(executions)
        }
      }
      analysisResultOpt = Some(analysisResult)
    }

  def getExecutionVolume(configurationOpt: Option[Configuration]): Option[ExecutionVolume] =
    for {
      analysisResult ← analysisResultOpt
      volume ← analysisResult.getExecutionVolume(configurationOpt)
    } yield volume

}