package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import org.joda.time._
import java.net.URI

/**
 * DAO for storing data related to CI (continuous integration) jobs & builds
 */
trait CiDao {

  /**
   * Run the given block within a transaction
   */
  def transaction[T](block: ⇒ T): T

  def ensureCiJob(job: CiJob): Id[CiJob]

  def newCiBuild(build: CiBuild)

  def getCiBuild(buildUrl: URI): Option[CiBuild]

  /**
   * @return all the URLs of builds imported from CI servers
   */
  def getCiBuildUrls(): Seq[URI]

  def getCiJobs(): Seq[CiJob]
  
  def getCiBuilds(specId: Id[CiImportSpec]): Seq[CiBuild]

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