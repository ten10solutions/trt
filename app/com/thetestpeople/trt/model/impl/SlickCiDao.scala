package com.thetestpeople.trt.model.impl

import org.joda.time._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import play.Logger
import java.net.URI

trait SlickCiDao { this: SlickDao ⇒

  import driver.simple._
  import Database.dynamicSession
  import jodaSupport._
  import Mappers._
  import Tables._

  private lazy val ciBuildInserter = ciBuilds.insertInvoker

  def newCiBuild(build: CiBuild) {
    ciBuildInserter.insert(build)
  }

  private lazy val getCiBuildCompiled = {
    def getCiBuild(buildUrl: Column[URI]) =
      ciBuilds.filter(_.buildUrl === buildUrl)
    Compiled(getCiBuild _)
  }

  def getCiBuild(buildUrl: URI): Option[CiBuild] = {
    getCiBuildCompiled(buildUrl).firstOption
  }

  def getCiBuildUrls(): Seq[URI] = ciBuilds.map(_.buildUrl).run

  def getCiBuilds(specId: Id[CiImportSpec]): Seq[CiBuild] = {
    val query =
      for {
        build ← ciBuilds
        if build.importSpecId === specId
      } yield build
    query.run
  }

  def getCiJobs(): Seq[CiJob] = ciJobs.run

  def newCiImportSpec(spec: CiImportSpec): Id[CiImportSpec] =
    (ciImportSpecs returning ciImportSpecs.map(_.id)).insert(spec)

  def getCiImportSpecs(): Seq[CiImportSpec] =
    ciImportSpecs.run

  def deleteCiImportSpec(id: Id[CiImportSpec]): Boolean = {
    val query = ciImportSpecs.filter(_.id === id)
    val deletedCount = query.delete
    deletedCount > 0
  }

  def getCiImportSpec(id: Id[CiImportSpec]): Option[CiImportSpec] =
    ciImportSpecs.filter(_.id === id).firstOption

  def updateCiImportSpec(updatedSpec: CiImportSpec) = {
    val query = for { spec ← ciImportSpecs if spec.id === updatedSpec.id } yield spec
    query.update(updatedSpec) > 0
  }

  /**
   * @return true if the import spec was updated, false if there was no import spec with the given ID
   */
  def updateCiImportSpec(id: Id[CiImportSpec], lastCheckedOpt: Option[DateTime]): Boolean = {
    val query =
      for {
        spec ← ciImportSpecs
        if spec.id === id
      } yield spec.lastChecked

    query.update(lastCheckedOpt) > 0
  }

  def getJenkinsConfiguration(): FullJenkinsConfiguration = {
    val config = jenkinsConfiguration.firstOption.getOrElse(
      throw new IllegalStateException("No Jenkins configuration present"))
    val params = jenkinsJobParams.run
    FullJenkinsConfiguration(config, params)
  }

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration): Unit = synchronized {
    jenkinsConfiguration.update(config.config)
    jenkinsJobParams.delete
    jenkinsJobParams.insertAll(config.params: _*)
  }

  def getTeamCityConfiguration(): TeamCityConfiguration =
    teamCityConfiguration.firstOption.getOrElse(
      throw new IllegalStateException("No TeamCity configuration present"))

  def updateTeamCityConfiguration(config: TeamCityConfiguration): Unit =
    teamCityConfiguration.update(config)

  def ensureCiJob(job: CiJob): Id[CiJob] = synchronized {
    ciJobs.filter(_.url === job.url).firstOption match {
      case Some(job) ⇒
        job.id
      case None ⇒
        (ciJobs returning ciJobs.map(_.id)).insert(job)
    }
  }

}