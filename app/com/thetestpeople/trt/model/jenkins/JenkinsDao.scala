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

  def newJenkinsBuild(build: CiBuild)

  def getJenkinsBuild(buildUrl: URI): Option[CiBuild]

  /**
   * @return all the URLs of builds imported from Jenkins
   */
  def getJenkinsBuildUrls(): Seq[URI]

  def getJenkinsJobs(): Seq[JenkinsJob]
  
  def getJenkinsBuilds(jobUrl: URI): Seq[CiBuild]

  def newCiImportSpec(spec: CiImportSpec): Id[CiImportSpec]

  def getCiImportSpecs(): Seq[CiImportSpec]

  def getCiImportSpec(id: Id[CiImportSpec]): Option[CiImportSpec]

  /**
   * @return true if the import spec was deleted, false if there was no import spec with the given ID
   */
  def deleteCiImportSpec(id: Id[CiImportSpec]): Boolean

  /**
   * @return true if the import spec was updated, false if there was no import spec with the given ID
   */
  def updateCiImportSpec(spec: CiImportSpec): Boolean

  /**
   * @return true if the import spec was updated, false if there was no import spec with the given ID
   */
  def updateCiImportSpec(id: Id[CiImportSpec], lastCheckedOpt: Option[DateTime]): Boolean

  def getJenkinsConfiguration(): FullJenkinsConfiguration

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration)

}