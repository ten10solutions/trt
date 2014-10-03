package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.utils.CoalescingBlockingQueue
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.utils.http.Credentials
import com.thetestpeople.trt.service.Clock
import com.thetestpeople.trt.service.BatchRecorder
import com.thetestpeople.trt.model.jenkins.JenkinsDao
import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model
import com.thetestpeople.trt.model.jenkins.JenkinsImportSpec
import com.thetestpeople.trt.model.Batch
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.model.jenkins.JenkinsConfiguration
import java.net.URI

class JenkinsImporter(
    clock: Clock,
    http: Http,
    dao: JenkinsDao,
    importStatusManager: JenkinsImportStatusManager,
    batchRecorder: BatchRecorder) extends HasLogger {

  import dao.transaction

  def importBuilds(specId: Id[JenkinsImportSpec]) {
    val specOpt = transaction(dao.getJenkinsImportSpec(specId))
    val spec = specOpt.getOrElse {
      logger.warn(s"No import spec found $specId, skipping")
      return
    }
    logger.debug(s"Examining ${spec.jobUrl} for new builds")
    importStatusManager.importStarted(spec.id, spec.jobUrl)
    try {
      doImportBuilds(spec)
      importStatusManager.importComplete(spec.id)
    } catch {
      case e: Exception ⇒
        logger.error(s"Problem importing from ${spec.jobUrl}", e)
        importStatusManager.importErrored(spec.id, e)
    }
  }

  private def doImportBuilds(spec: JenkinsImportSpec) {
    val alreadyImportedBuildUrls = transaction { dao.getJenkinsBuildUrls() }.toSet
    def alreadyImported(link: JenkinsBuildLink) = alreadyImportedBuildUrls.contains(link.buildUrl)
    val jenkinsScraper = getJenkinsScraper(spec.importConsoleLog)

    val job = jenkinsScraper.getJenkinsJob(spec.jobUrl)

    val buildLinks = job.buildLinks.filterNot(alreadyImported).sortBy(_.buildNumber).reverse

    for (link ← buildLinks)
      importStatusManager.buildExists(spec.id, link.buildUrl, link.buildNumber)
    for (link ← buildLinks)
      importBuild(link, job, spec, jenkinsScraper)

    transaction { dao.updateJenkinsImportSpec(spec.id, Some(clock.now)) }
  }

  private def getJenkinsScraper(importConsoleLog: Boolean): JenkinsScraper = {
    val jenkinsConfiguration = transaction { dao.getJenkinsConfiguration() }
    val credentialsOpt = jenkinsConfiguration.config.credentialsOpt
    new JenkinsScraper(http, credentialsOpt, importConsoleLog)
  }

  private def importBuild(buildLink: JenkinsBuildLink, job: JenkinsJob, importSpec: JenkinsImportSpec, jenkinsScraper: JenkinsScraper) {
    val buildUrl = buildLink.buildUrl
    logger.debug(s"Importing build $buildUrl")
    importStatusManager.buildStarted(importSpec.id, buildUrl)
    try {
      val batchIdOpt = doImportBuild(buildLink, job, importSpec, jenkinsScraper)
      importStatusManager.buildComplete(importSpec.id, buildUrl, batchIdOpt)
    } catch {
      case e: Exception ⇒
        logger.error(s"Problem importing from $buildUrl", e)
        importStatusManager.buildErrored(importSpec.id, buildUrl, e)
    }
  }

  /**
   * @return None if build had no associated test executions. 
   */
  private def doImportBuild(buildLink: JenkinsBuildLink, job: JenkinsJob, importSpec: JenkinsImportSpec, jenkinsScraper: JenkinsScraper): Option[Id[Batch]] = {
    val buildUrl = buildLink.buildUrl
    val build = jenkinsScraper.scrapeBuild(buildUrl, importSpec.jobUrl)
      .getOrElse(return None)

    val batch = new JenkinsBatchCreator(importSpec.configurationOpt).createBatch(build)
    val batchId = batchRecorder.recordBatch(batch).id

    val modelJob = model.jenkins.JenkinsJob(url = importSpec.jobUrl, name = job.name)
    val jobId = transaction { dao.ensureJenkinsJob(modelJob) }
    val modelBuild = model.jenkins.JenkinsBuild(batchId, clock.now, buildUrl, buildLink.buildNumber, jobId, Some(importSpec.id))
    transaction { dao.newJenkinsBuild(modelBuild) }
    Some(batchId)
  }

}