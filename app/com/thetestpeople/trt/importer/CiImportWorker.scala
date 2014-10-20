package com.thetestpeople.trt.importer

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.CoalescingBlockingQueue
import com.thetestpeople.trt.utils.HasLogger

trait CiImportQueue {

  /**
   * Enqueue the given import spec to be imported
   */
  def add(importSpecId: Id[CiImportSpec])

}

/**
 * Examine Jenkins jobs to examine for any new builds to import.
 */
class CiImportWorker(
    dao: CiDao,
    ciImporter: CiImporter) extends CiImportQueue with HasLogger {

  import dao.transaction

  private val importSpecQueue: CoalescingBlockingQueue[Id[CiImportSpec]] = new CoalescingBlockingQueue

  private var continue = true

  def add(importSpecId: Id[CiImportSpec]) {
    logger.debug(s"Queued import for $importSpecId")
    importSpecQueue.offer(importSpecId)
  }

  def run() {
    logger.debug("CI import worker started")
    while (continue) {
      val specId = importSpecQueue.take()
      logger.info(s"Checking if there is anything to import from import spec $specId")
      try
        ciImporter.importBuilds(specId)
      catch {
        case e: Exception ⇒ logger.error(s"Problem importing from import spec $specId", e)
      }
    }
    logger.debug("CI import worker finished")
  }

  def stop() {
    logger.debug("CI Jenkins import worker")
    continue = false
  }

}