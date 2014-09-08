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

class JenkinsImportWorker(
    clock: Clock,
    http: Http,
    dao: JenkinsDao,
    importStatusManager: JenkinsImportStatusManager,
    batchRecorder: BatchRecorder) extends JenkinsImportQueue with HasLogger {

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
      val importSpecId = importSpecQueue.take()
      logger.info(s"Checking if there is anything to import from import spec $importSpecId")
      try {
        val importSpecOpt = transaction(dao.getJenkinsImportSpec(importSpecId))
        importSpecOpt match {
          case Some(importSpec) ⇒ importBuilds(importSpec)
          case None             ⇒ logger.warn(s"No import spec found $importSpecId, skipping")
        }
      } catch {
        case e: Exception ⇒ logger.error("Problem importing from import spec $importSpecId", e)
      }
    }
    logger.debug("Jenkins import worker finished")
  }

  private def importBuilds(importSpec: JenkinsImportSpec) {
    logger.debug("Examining ${importSpec.jobUrl} for new builds")
    importStatusManager.importStarted(importSpec.id, importSpec.jobUrl)
    try {
      val alreadyImportedBuildUrls = transaction { dao.getJenkinsBuildUrls() }.toSet
      def alreadyImported(buildLink: JenkinsBuildLink) = alreadyImportedBuildUrls.contains(buildLink.buildUrl)

      val jenkinsConfiguration = transaction { dao.getJenkinsConfiguration() }
      val credentialsOpt = jenkinsConfiguration.config.credentialsOpt
      val jenkinsScraper = new JenkinsScraper(http, credentialsOpt, importSpec.importConsoleLog, alreadyImportedBuildUrls)

      val job = jenkinsScraper.getJenkinsJob(importSpec.jobUrl)
        .getOrElse(throw new RuntimeException("Couldn't download job info"))

      val buildLinks = job.buildLinks
        .filterNot(alreadyImported)
        .sortBy(_.buildNumber)
        .reverse

      for (buildLink ← buildLinks)
        importBuild(buildLink, job, importSpec, jenkinsScraper)

      transaction { dao.updateJenkinsImportSpec(importSpec.id, Some(clock.now)) }

      importStatusManager.importComplete(importSpec.id)
    } catch {
      case e: Exception ⇒
        logger.error(s"Problem importing from ${importSpec.jobUrl}", e)
        importStatusManager.importErrored(importSpec.id, e)
    }
  }

  private def importBuild(buildLink: JenkinsBuildLink, job: JenkinsJob, importSpec: JenkinsImportSpec, jenkinsScraper: JenkinsScraper) {
    val buildUrl = buildLink.buildUrl
    logger.debug(s"Importing build $buildUrl")
    importStatusManager.buildStarted(importSpec.id, buildUrl, buildLink.buildNumber)
    try {
      val build = jenkinsScraper.scrapeBuild(buildUrl, importSpec.jobUrl)
        .getOrElse(throw new RuntimeException("Couldn't download build info"))

      val batch = new JenkinsBatchCreator(importSpec.configurationOpt).createBatch(build)
      val batchId = batchRecorder.recordBatch(batch).id

      val modelJob = model.jenkins.JenkinsJob(url = importSpec.jobUrl, name = job.name)
      val jobId = dao.ensureJenkinsJob(modelJob)
      dao.newJenkinsBuild(model.jenkins.JenkinsBuild(batchId, clock.now, buildUrl, jobId))

      importStatusManager.buildComplete(importSpec.id, buildUrl, batchId, numberOfExecutions = batch.executions.size)
    } catch {
      case e: Exception ⇒
        logger.error(s"Problem importing from $buildUrl", e)
        importStatusManager.buildErrored(importSpec.id, buildUrl, e)
    }
  }

  def stop() {
    continue = false
  }

}