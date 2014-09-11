package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import org.joda.time._
import java.net.URI

/**
 * DAO for storing Jenkins related data
 */
trait JenkinsDao {

  /**
   * Run the given block within a transaction
   */
  def transaction[T](block: ⇒ T): T

  def ensureJenkinsJob(job: JenkinsJob): Id[JenkinsJob]

  def newJenkinsBuild(jenkinsBuild: JenkinsBuild)

  def getJenkinsBuild(buildUrl: URI): Option[JenkinsBuild]

  /**
   * @return all the URLs of builds imported from Jenkins
   */
  def getJenkinsBuildUrls(): List[URI]

  def getJenkinsJobs(): List[JenkinsJob]
  
  def getJenkinsBuilds(jobUrl: URI): Seq[JenkinsBuild]

  def newJenkinsImportSpec(spec: JenkinsImportSpec): Id[JenkinsImportSpec]

  def getJenkinsImportSpecs(): List[JenkinsImportSpec]

  def getJenkinsImportSpec(id: Id[JenkinsImportSpec]): Option[JenkinsImportSpec]

  /**
   * @return true if the import spec was deleted, false if there was no import spec with the given ID
   */
  def deleteJenkinsImportSpec(id: Id[JenkinsImportSpec]): Boolean

  /**
   * @return true if the import spec was updated, false if there was no import spec with the given ID
   */
  def updateJenkinsImportSpec(spec: JenkinsImportSpec): Boolean

  /**
   * @return true if the import spec was updated, false if there was no import spec with the given ID
   */
  def updateJenkinsImportSpec(id: Id[JenkinsImportSpec], lastCheckedOpt: Option[DateTime]): Boolean

  def getJenkinsConfiguration(): FullJenkinsConfiguration

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration)

}