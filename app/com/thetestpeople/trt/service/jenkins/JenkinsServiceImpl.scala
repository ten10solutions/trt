package com.thetestpeople.trt.service.jenkins

import java.net.URI
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.KeyedLocks
import com.thetestpeople.trt.jenkins.importer
import com.thetestpeople.trt.jenkins.importer.JenkinsScraper
import com.thetestpeople.trt.jenkins.importer.JenkinsBuildImportStatus
import com.thetestpeople.trt.jenkins.importer.JenkinsJobImportStatus
import com.thetestpeople.trt.jenkins.importer.JenkinsBatchCreator
import com.thetestpeople.trt.service.jenkins._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.jenkins.trigger.JenkinsTestRunner
import com.thetestpeople.trt.utils.KeyedLocks

trait JenkinsServiceImpl extends JenkinsService { self: ServiceImpl ⇒

  import dao.transaction

  def getCiImportSpecs: Seq[CiImportSpec] = transaction { dao.getCiImportSpecs }

  def newCiImportSpec(spec: CiImportSpec) =
    transaction {
      val specId = dao.newCiImportSpec(spec.copy(lastCheckedOpt = None))
      logger.info(s"Added new import spec for ${spec.jobUrl}, id = ${spec.id}")
      jenkinsImportQueue.add(specId)
      specId
    }

  def deleteCiImportSpec(id: Id[CiImportSpec]) = transaction {
    val success = dao.deleteCiImportSpec(id)
    if (success)
      logger.info(s"Deleted Jenkins import spec $id")
    success
  }

  def getCiImportSpec(id: Id[CiImportSpec]): Option[CiImportSpec] = transaction {
    dao.getCiImportSpec(id)
  }

  def updateCiImportSpec(spec: CiImportSpec): Boolean =
    transaction {
      val success = dao.updateCiImportSpec(spec)
      if (success)
        logger.info(s"Updated Jenkins import spec $spec.id")
      success
    }

  def syncJenkins(specId: Id[CiImportSpec]) = {
    jenkinsImportQueue.add(specId)
  }

  def syncAllJenkins() {
    logger.debug("syncAllJenkins()")
    val specs = transaction { dao.getCiImportSpecs() }
    val now = clock.now
    def isOverdue(spec: CiImportSpec) = spec.nextCheckDueOpt.forall(_ <= now)
    for (spec ← specs if isOverdue(spec))
      syncJenkins(spec.id)

  }

  def getJenkinsConfiguration(): FullJenkinsConfiguration = transaction { dao.getJenkinsConfiguration() }

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration) = transaction {
    dao.updateJenkinsConfiguration(config)
    logger.info(s"Updated Jenkins configuration to $config")
  }

  def rerunTests(testIds: Seq[Id[Test]]) = transaction {
    new JenkinsTestRunner(dao, http).rerunTests(testIds)
  }

  def canRerun: Boolean = transaction {
    dao.getJenkinsConfiguration.config.rerunJobUrlOpt.isDefined
  }

  def getJenkinsJobs(): Seq[JenkinsJob] = transaction { dao.getJenkinsJobs() }

  def getJenkinsBuilds(jobUrl: URI): Seq[CiBuild] = transaction { dao.getJenkinsBuilds(jobUrl) }

  def getBuildImportStatuses(specId: Id[CiImportSpec]): Seq[JenkinsBuildImportStatus] = {
    jenkinsImportStatusManager.getBuildImportStatuses(specId)
  }
  
  def getJobImportStatus(specId: Id[CiImportSpec]): Option[JenkinsJobImportStatus] = {
    jenkinsImportStatusManager.getJobImportStatus(specId)
  }

}