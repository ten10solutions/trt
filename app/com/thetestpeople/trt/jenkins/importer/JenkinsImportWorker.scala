package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model
import com.thetestpeople.trt.utils.CoalescingBlockingQueue
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.service.Clock
import com.thetestpeople.trt.model.jenkins.JenkinsDao
import com.thetestpeople.trt.model.jenkins.JenkinsImportSpec
import play.core.BuildLink
import com.thetestpeople.trt.model.Batch
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.service.BatchRecorder
import com.thetestpeople.trt.model.jenkins.JenkinsConfiguration

trait JenkinsImportQueue {

  /**
   * Enqueue the given import spec to be imported
   */
  def add(importSpecId: Id[JenkinsImportSpec])

}

/**
 * Examine Jenkins jobs to examine for any new builds to import.
 */
class JenkinsImportWorker(
    dao: JenkinsDao,
    jenkinsImporter: JenkinsImporter) extends JenkinsImportQueue with HasLogger {

  import dao.transaction

  private val importSpecQueue: CoalescingBlockingQueue[Id[JenkinsImportSpec]] = new CoalescingBlockingQueue

  private var continue = true

  def add(importSpecId: Id[JenkinsImportSpec]) {
    logger.debug(s"Queued import for $importSpecId")
    importSpecQueue.offer(importSpecId)
  }

  def run() {
    logger.debug("Jenkins import worker started")
    while (continue) {
      val specId = importSpecQueue.take()
      logger.info(s"Checking if there is anything to import from import spec $specId")
      try
        jenkinsImporter.importBuilds(specId)
      catch {
        case e: Exception ⇒ logger.error(s"Problem importing from import spec $specId", e)
      }
    }
    logger.debug("Jenkins import worker finished")
  }

  def stop() {
    logger.debug("Stopping Jenkins import worker")
    continue = false
  }

}