package com.thetestpeople.trt.service.jenkins

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.jenkins.trigger.TriggerResult

trait JenkinsService {

  /**
   * Add a new Jenkins import spec.
   *
   * @param spec -- any value set for id or lastCheckedOpt will be ignored.
   */
  def newJenkinsImportSpec(spec: JenkinsImportSpec): Id[JenkinsImportSpec]

  def updateJenkinsImportSpec(spec: JenkinsImportSpec): Boolean

  def getJenkinsImportSpec(id: Id[JenkinsImportSpec]): Option[JenkinsImportSpec]

  def getJenkinsImportSpecs: List[JenkinsImportSpec]

  /**
   * @return true if the import spec was deleted, false if there was no import spec with the given ID
   */
  def deleteJenkinsImportSpec(id: Id[JenkinsImportSpec]): Boolean

  /**
   * @return true if the import spec was synced, false if there was no import spec with the given ID
   */
  def syncJenkins(id: Id[JenkinsImportSpec]): Boolean

  def syncAllJenkins()

  def getJenkinsConfiguration(): FullJenkinsConfiguration

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration)

  def rerunTests(testIds: List[Id[Test]]): TriggerResult

  /**
   * Whether or not there is configuration to rerun tests through Jenkins
   */
  def canRerun: Boolean

  def getJenkinsJobs(): List[JenkinsJob]

}