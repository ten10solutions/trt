package com.thetestpeople.trt.importer.jenkins

import com.thetestpeople.trt.model.Batch
import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.CiBuild
import com.thetestpeople.trt.model.jenkins.CiDao
import com.thetestpeople.trt.model.jenkins.CiImportSpec
import com.thetestpeople.trt.model.jenkins.CiJob
import com.thetestpeople.trt.service.BatchRecorder
import com.thetestpeople.trt.service.Clock
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.UriUtils._
import com.thetestpeople.trt.utils.http.Http
import com.thetestpeople.trt.importer.CiImportStatusManager
import java.net.URI
import org.apache.http.client.utils.URIBuilder

class JenkinsImporter(clock: Clock,
    http: Http,
    dao: CiDao,
    importStatusManager: CiImportStatusManager,
    batchRecorder: BatchRecorder) extends HasLogger {

  import dao.transaction

  def importBuilds(spec: CiImportSpec) {
    val alreadyImportedBuildUrls = transaction { dao.getCiBuildUrls() }.toSet
    def alreadyImported(link: JenkinsBuildLink) = alreadyImportedBuildUrls contains link.buildUrl
    val buildDownloader = getJenkinsBuildDownloader(spec.importConsoleLog)

    val job = buildDownloader.getJenkinsJob(spec.jobUrl)

    val buildLinks = job.buildLinks.filterNot(alreadyImported).sortBy(_.buildNumber).reverse

    for (link ← buildLinks)
      importStatusManager.buildExists(spec.id, link.buildUrl, Some(link.buildNumber))
    for (link ← buildLinks)
      importBuild(link, job, spec, buildDownloader)

    transaction { dao.updateCiImportSpec(spec.id, Some(clock.now)) }
  }

  /**
   * Update links returned by jenkins to use the same host and port as the original job link (in case it gets returned
   * as something different by the Jenkins API).
   */
  private def updateJenkinsUrl(jobUrl: URI)(otherUrl: URI): URI =
    otherUrl.withSameHostAndPortAs(jobUrl)

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

    val ciJob = CiJob(url = job.url, name = job.name)
    val jobId = transaction { dao.ensureCiJob(ciJob) }
    val ciBuild = CiBuild(batchId = batchId, importTime = clock.now, buildUrl = buildUrl,
      buildNumberOpt = Some(buildLink.buildNumber), buildNameOpt = build.buildSummary.nameOpt, jobId = jobId,
      importSpecIdOpt = Some(importSpec.id))
    transaction { dao.newCiBuild(ciBuild) }
    Some(batchId)
  }

}