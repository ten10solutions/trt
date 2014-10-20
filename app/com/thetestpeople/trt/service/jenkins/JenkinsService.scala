package com.thetestpeople.trt.service.jenkins

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.jenkins.trigger.TriggerResult
import java.net.URI
import com.thetestpeople.trt.importer._

trait JenkinsService {

  /**
   * Add a new Jenkins import spec.
   *
   * @param spec -- any value set for id or lastCheckedOpt will be ignored.
   */
  def newCiImportSpec(spec: CiImportSpec): Id[CiImportSpec]

  def updateCiImportSpec(spec: CiImportSpec): Boolean

  def getCiImportSpec(id: Id[CiImportSpec]): Option[CiImportSpec]

  def getCiImportSpecs: Seq[CiImportSpec]

  /**
   * @return true if the import spec was deleted, false if there was no import spec with the given ID
   */
  def deleteCiImportSpec(id: Id[CiImportSpec]): Boolean

  /**
   * Schedule the given Jenkins job to be scanned for builds to import
   */
  def syncJenkins(id: Id[CiImportSpec])

  /**
   * Schedule all the Jenkins jobs to be scanned for builds to import
   */
  def syncAllJenkins()

  def getJenkinsConfiguration(): FullJenkinsConfiguration

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration)

  def rerunTests(testIds: Seq[Id[Test]]): TriggerResult

  /**
   * Whether or not there is configuration to rerun tests through Jenkins
   */
  def canRerun: Boolean

  def getCiJobs(): Seq[CiJob]

  def getCiBuilds(specId: Id[CiImportSpec]): Seq[CiBuild]

  def getBuildImportStatuses(specId: Id[CiImportSpec]): Seq[CiBuildImportStatus]

  def getJobImportStatus(specId: Id[CiImportSpec]): Option[CiJobImportStatus]
}