package com.thetestpeople.trt.service.jenkins

import java.net.URI
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.KeyedLocks
import com.thetestpeople.trt.jenkins.importer
import com.thetestpeople.trt.jenkins.importer.JenkinsScraper
import com.thetestpeople.trt.jenkins.importer.JenkinsBatchCreator
import com.thetestpeople.trt.service.jenkins._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.jenkins.trigger.JenkinsTestRunner
import com.thetestpeople.trt.utils.KeyedLocks

trait JenkinsServiceImpl extends JenkinsService { self: ServiceImpl ⇒

  import dao.transaction

  private def importNewJenkinsBuilds(specId: Id[JenkinsImportSpec], jobUrl: URI, importConsoleLog: Boolean, configurationOpt: Option[Configuration]) {
    val alreadyImportedBuildUrls = transaction { dao.getJenkinsBuildUrls().toSet }
    val jenkinsConfiguration = transaction { dao.getJenkinsConfiguration() }
    val credentialsOpt = jenkinsConfiguration.config.credentialsOpt
    val jenkinsScraper = new JenkinsScraper(http, credentialsOpt, importConsoleLog, alreadyImportedBuildUrls)
    val builds = jenkinsScraper.scrapeBuildsFromJob(jobUrl)(tryImportJenkinsBuild(configurationOpt) _)
    transaction { dao.updateJenkinsImportSpec(specId, Some(clock.now)) }
  }

  private val importLocks = new KeyedLocks[URI]

  private def tryImportJenkinsBuild(configurationOpt: Option[Configuration])(job: importer.JenkinsJob, build: importer.JenkinsBuild) = {
    val buildUrl = build.buildSummary.url
    val successOpt = importLocks.tryLock(buildUrl) {
      transaction {
        dao.getJenkinsBuild(buildUrl) match {
          case Some(importedBuild) ⇒
            logger.debug(s"Already imported $buildUrl, skipping")
          case None ⇒
            importJenkinsBuild(job, build, configurationOpt)
        }
      }
    }
    if (successOpt.isEmpty)
      logger.debug(s"Build $buildUrl is in the process of being imported, skipping")
  }

  private def importJenkinsBuild(job: importer.JenkinsJob, build: importer.JenkinsBuild, configurationOpt: Option[Configuration]) {
    val buildUrl = build.buildSummary.url
    val batch = new JenkinsBatchCreator(configurationOpt).createBatch(build)
    val batchId = batchRecorder.recordBatch(batch).id
    val jobId = dao.ensureJenkinsJob(JenkinsJob(url = job.url, name = job.name))
    dao.newJenkinsBuild(JenkinsBuild(batchId, clock.now, buildUrl, jobId))
  }

  def getJenkinsImportSpecs: List[JenkinsImportSpec] = transaction { dao.getJenkinsImportSpecs }

  def newJenkinsImportSpec(spec: JenkinsImportSpec) =
    transaction {
      val specId = dao.newJenkinsImportSpec(spec.copy(lastCheckedOpt = None))
      logger.info(s"Added new import spec for ${spec.jobUrl}, id = ${spec.id}")
      specId
    }

  def deleteJenkinsImportSpec(id: Id[JenkinsImportSpec]) = transaction {
    val success = dao.deleteJenkinsImportSpec(id)
    if (success)
      logger.info(s"Deleted Jenkins import spec $id")
    success
  }

  def getJenkinsImportSpec(id: Id[JenkinsImportSpec]): Option[JenkinsImportSpec] = transaction {
    dao.getJenkinsImportSpec(id)
  }

  def updateJenkinsImportSpec(spec: JenkinsImportSpec): Boolean =
    transaction {
      val success = dao.updateJenkinsImportSpec(spec)
      if (success)
        logger.info(s"Updated Jenkins import spec $spec.id")
      success
    }

  def syncJenkins(specId: Id[JenkinsImportSpec]) = {
    jenkinsImportQueue.add(specId)
    true
  }

  def syncAllJenkins() {
    logger.debug("syncAllJenkins()")
    val specs = transaction { dao.getJenkinsImportSpecs() }
    val now = clock.now
    def isOverdue(spec: JenkinsImportSpec) = spec.nextCheckDueOpt.forall(_ <= now)
    for (spec ← specs if isOverdue(spec))
      syncJenkins(spec.id)

  }

  def getJenkinsConfiguration(): FullJenkinsConfiguration = transaction { dao.getJenkinsConfiguration() }

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration) = transaction {
    dao.updateJenkinsConfiguration(config)
    logger.info(s"Updated Jenkins configuration to $config")
  }

  def rerunTests(testIds: List[Id[Test]]) = transaction {
    new JenkinsTestRunner(dao, http).rerunTests(testIds)
  }

  def canRerun: Boolean = transaction {
    dao.getJenkinsConfiguration.config.rerunJobUrlOpt.isDefined
  }

  def getJenkinsJobs(): List[JenkinsJob] = transaction { dao.getJenkinsJobs() }

}