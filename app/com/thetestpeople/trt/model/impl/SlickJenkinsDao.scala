package com.thetestpeople.trt.model.impl

import org.joda.time._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import play.Logger
import java.net.URI

trait SlickJenkinsDao { this: SlickDao ⇒

  import driver.simple._
  import Database.dynamicSession
  import jodaSupport._
  import Mappers._
  import Tables._

  private lazy val jenkinsBuildInserter = jenkinsBuilds.insertInvoker

  def newJenkinsBuild(jenkinsBuild: JenkinsBuild) {
    jenkinsBuildInserter.insert(jenkinsBuild)
  }

  private lazy val getJenkinsBuildCompiled = {
    def getJenkinsBuild(buildUrl: Column[URI]) =
      jenkinsBuilds.filter(_.buildUrl === buildUrl)
    Compiled(getJenkinsBuild _)
  }

  def getJenkinsBuild(buildUrl: URI): Option[JenkinsBuild] = {
    getJenkinsBuildCompiled(buildUrl).firstOption
  }

  def getJenkinsBuildUrls(): Seq[URI] = jenkinsBuilds.map(_.buildUrl).run

  def getJenkinsBuilds(jobUrl: URI): Seq[JenkinsBuild] = {
    val query =
      for {
        job ← jenkinsJobs
        build ← jenkinsBuilds
        if build.jobId === job.id
        if job.url === jobUrl
      } yield build
    query.run
  }

  def getJenkinsJobs(): Seq[JenkinsJob] = jenkinsJobs.run

  def newJenkinsImportSpec(spec: JenkinsImportSpec): Id[JenkinsImportSpec] =
    (jenkinsImportSpecs returning jenkinsImportSpecs.map(_.id)).insert(spec)

  def getJenkinsImportSpecs(): Seq[JenkinsImportSpec] =
    jenkinsImportSpecs.run

  def deleteJenkinsImportSpec(id: Id[JenkinsImportSpec]): Boolean = {
    val query = jenkinsImportSpecs.filter(_.id === id)
    val deletedCount = query.delete
    deletedCount > 0
  }

  def getJenkinsImportSpec(id: Id[JenkinsImportSpec]): Option[JenkinsImportSpec] =
    jenkinsImportSpecs.filter(_.id === id).firstOption

  def updateJenkinsImportSpec(updatedSpec: JenkinsImportSpec) = {
    val query = for { spec ← jenkinsImportSpecs if spec.id === updatedSpec.id } yield spec
    query.update(updatedSpec) > 0
  }

  /**
   * @return true if the import spec was updated, false if there was no import spec with the given ID
   */
  def updateJenkinsImportSpec(id: Id[JenkinsImportSpec], lastCheckedOpt: Option[DateTime]): Boolean = {
    val query =
      for {
        spec ← jenkinsImportSpecs
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

  def ensureJenkinsJob(job: JenkinsJob): Id[JenkinsJob] = synchronized {
    jenkinsJobs.filter(_.url === job.url).firstOption match {
      case Some(job) ⇒
        job.id
      case None ⇒
        (jenkinsJobs returning jenkinsJobs.map(_.id)).insert(job)
    }
  }

}