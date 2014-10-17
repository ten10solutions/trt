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
import com.thetestpeople.trt.model.jenkins.CiImportSpec
import com.thetestpeople.trt.model.jenkins.CiBuild
import com.thetestpeople.trt.model.jenkins.CiJob
import com.thetestpeople.trt.model.Batch
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.model.jenkins.JenkinsConfiguration
import java.net.URI
import com.thetestpeople.trt.model.CiType._
import com.thetestpeople.trt.model.CiType

class CiImporter(
    clock: Clock,
    http: Http,
    dao: JenkinsDao,
    importStatusManager: JenkinsImportStatusManager,
    batchRecorder: BatchRecorder) extends HasLogger {

  import dao.transaction

  def importBuilds(specId: Id[CiImportSpec]) {
    val specOpt = transaction(dao.getCiImportSpec(specId))
    val spec = specOpt.getOrElse {
      logger.warn(s"No import spec found $specId, skipping")
      return
    }
    logger.debug(s"Examining ${spec.jobUrl} for new builds")
    importStatusManager.importStarted(spec.id, spec.jobUrl)
    try {
      spec.ciType match {
        case CiType.Jenkins ⇒ importJenkinsBuilds(spec)
        //        case CiType.TeamCity ⇒ doImportBuilds(spec)
        case t              ⇒ logger.warn(s"Unknown CI type $t for spec $specId, skipping")
      }
      importStatusManager.importComplete(spec.id)
    } catch {
      case e: Exception ⇒
        logger.error(s"Problem importing from ${spec.jobUrl}", e)
        importStatusManager.importErrored(spec.id, e)
    }
  }

  private def importJenkinsBuilds(spec: CiImportSpec) {
    val alreadyImportedBuildUrls = transaction { dao.getCiBuildUrls() }.toSet
    def alreadyImported(link: JenkinsBuildLink) = alreadyImportedBuildUrls contains link.buildUrl
    val jenkinsBuildDownloader = getJenkinsBuildDownloader(spec.importConsoleLog)

    val job = jenkinsBuildDownloader.getJenkinsJob(spec.jobUrl)

    val buildLinks = job.buildLinks.filterNot(alreadyImported).sortBy(_.buildNumber).reverse

    for (link ← buildLinks)
      importStatusManager.buildExists(spec.id, link.buildUrl, link.buildNumber)
    for (link ← buildLinks)
      importBuild(link, job, spec, jenkinsBuildDownloader)

    transaction { dao.updateCiImportSpec(spec.id, Some(clock.now)) }
  }

  private def getJenkinsBuildDownloader(importConsoleLog: Boolean): JenkinsBuildDownloader = {
    val jenkinsConfiguration = transaction { dao.getJenkinsConfiguration() }
    val credentialsOpt = jenkinsConfiguration.config.credentialsOpt
    new JenkinsBuildDownloader(http, credentialsOpt, importConsoleLog)
  }

  private def importBuild(buildLink: JenkinsBuildLink, job: JenkinsJob, importSpec: CiImportSpec, jenkinsBuildDownloader: JenkinsBuildDownloader) {
    val buildUrl = buildLink.buildUrl
    importStatusManager.buildStarted(importSpec.id, buildUrl)
    try {
      val batchIdOpt = doImportBuild(buildLink, job, importSpec, jenkinsBuildDownloader)
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
  private def doImportBuild(buildLink: JenkinsBuildLink, job: JenkinsJob, importSpec: CiImportSpec, jenkinsBuildDownloader: JenkinsBuildDownloader): Option[Id[Batch]] = {
    val buildUrl = buildLink.buildUrl
    val build = jenkinsBuildDownloader.getJenkinsBuild(buildUrl, importSpec.jobUrl)
      .getOrElse(return None)

    val batch = new JenkinsBatchCreator(importSpec.configurationOpt).createBatch(build)
    val batchId = batchRecorder.recordBatch(batch).id

    val modelJob = CiJob(url = importSpec.jobUrl, name = job.name)
    val jobId = transaction { dao.ensureCiJob(modelJob) }
    val ciBuild = CiBuild(batchId, clock.now, buildUrl, buildLink.buildNumber, jobId, Some(importSpec.id))
    transaction { dao.newCiBuild(ciBuild) }
    Some(batchId)
  }

}