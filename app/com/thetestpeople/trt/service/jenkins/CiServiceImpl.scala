package com.thetestpeople.trt.service.jenkins

import java.net.URI
import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.KeyedLocks
import com.thetestpeople.trt.importer.jenkins._
import com.thetestpeople.trt.importer._
import com.thetestpeople.trt.service.jenkins._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.jenkins.trigger.JenkinsTestRunner
import com.thetestpeople.trt.utils.KeyedLocks

trait CiServiceImpl extends CiService { self: ServiceImpl ⇒

  import dao.transaction

  def getCiImportSpecs: Seq[CiImportSpec] = transaction { dao.getCiImportSpecs }

  def newCiImportSpec(spec: CiImportSpec) =
    transaction {
      val specId = dao.newCiImportSpec(spec.copy(lastCheckedOpt = None))
      logger.info(s"Added new import spec for ${spec.jobUrl}, id = ${spec.id}")
      ciImportQueue.add(specId)
      specId
    }

  def deleteCiImportSpec(id: Id[CiImportSpec]) = transaction {
    val success = dao.deleteCiImportSpec(id)
    if (success)
      logger.info(s"Deleted CI import spec $id")
    success
  }

  def getCiImportSpec(id: Id[CiImportSpec]): Option[CiImportSpec] = transaction {
    dao.getCiImportSpec(id)
  }

  def updateCiImportSpec(spec: CiImportSpec): Boolean =
    transaction {
      val success = dao.updateCiImportSpec(spec)
      if (success)
        logger.info(s"Updated CI import spec $spec.id")
      success
    }

  def syncCiImport(specId: Id[CiImportSpec]) = {
    ciImportQueue.add(specId)
  }

  def syncAllCiImports() {
    logger.debug("syncAllCiImports()")
    val specs = transaction { dao.getCiImportSpecs() }
    val now = clock.now
    def isOverdue(spec: CiImportSpec) = spec.nextCheckDueOpt.forall(_ <= now)
    for (spec ← specs if isOverdue(spec))
      syncCiImport(spec.id)
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

  def getCiJobs(): Seq[CiJob] = transaction { dao.getCiJobs() }

  def getCiBuilds(specId: Id[CiImportSpec]): Seq[CiBuild] = transaction { dao.getCiBuilds(specId) }

  def getBuildImportStatuses(specId: Id[CiImportSpec]): Seq[CiBuildImportStatus] = {
    ciImportStatusManager.getBuildImportStatuses(specId)
  }

  def getJobImportStatus(specId: Id[CiImportSpec]): Option[CiJobImportStatus] = {
    ciImportStatusManager.getJobImportStatus(specId)
  }

  def getTeamCityConfiguration(): TeamCityConfiguration = transaction { dao.getTeamCityConfiguration }

  def updateTeamCityConfiguration(config: TeamCityConfiguration) = transaction {
    dao.updateTeamCityConfiguration(config)
    logger.info(s"Updated TeamCity configuration to $config")
  }

}