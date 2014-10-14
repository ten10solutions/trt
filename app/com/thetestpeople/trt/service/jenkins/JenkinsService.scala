package com.thetestpeople.trt.service.jenkins

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.jenkins.trigger.TriggerResult
import java.net.URI
import com.thetestpeople.trt.jenkins.importer.JenkinsBuildImportStatus
import com.thetestpeople.trt.jenkins.importer.JenkinsJobImportStatus

trait JenkinsService {

  /**
   * Add a new Jenkins import spec.
   *
   * @param spec -- any value set for id or lastCheckedOpt will be ignored.
   */
  def newJenkinsImportSpec(spec: JenkinsImportSpec): Id[JenkinsImportSpec]

  def updateJenkinsImportSpec(spec: JenkinsImportSpec): Boolean

  def getJenkinsImportSpec(id: Id[JenkinsImportSpec]): Option[JenkinsImportSpec]

  def getJenkinsImportSpecs: Seq[JenkinsImportSpec]

  /**
   * @return true if the import spec was deleted, false if there was no import spec with the given ID
   */
  def deleteJenkinsImportSpec(id: Id[JenkinsImportSpec]): Boolean

  /**
   * Schedule the given Jenkins job to be scanned for builds to import
   */
  def syncJenkins(id: Id[JenkinsImportSpec])

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

  def getJenkinsJobs(): Seq[JenkinsJob]

  def getJenkinsBuilds(jobUrl: URI): Seq[JenkinsBuild]

  def getBuildImportStatuses(specId: Id[JenkinsImportSpec]): Seq[JenkinsBuildImportStatus]

  def getJobImportStatus(specId: Id[JenkinsImportSpec]): Option[JenkinsJobImportStatus]
}