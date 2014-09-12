package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.LockUtils._
import com.thetestpeople.trt.service.Clock
import java.net.URI
import java.net.URL
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import org.joda.time.DateTime

/**
 * Stores information about the state of ongoing Jenkins imports.
 */
class JenkinsImportStatusManager(clock: Clock) {

  private val lock: Lock = new ReentrantLock

  private var specStatuses: Map[Id[JenkinsImportSpec], MutableJenkinsSpecImportStatus] = Map()

  def importStarted(id: Id[JenkinsImportSpec], jobUrl: URI) = lock.withLock {
    specStatuses += id -> new MutableJenkinsSpecImportStatus(id, jobUrl)
    specStatuses(id).started()
  }

  def importComplete(id: Id[JenkinsImportSpec]) = lock.withLock {
    specStatuses(id).complete()
  }

  def importErrored(id: Id[JenkinsImportSpec], t: Throwable) = lock.withLock {
    specStatuses(id).errored(t)
  }

  def getBuildImportStatuses(id: Id[JenkinsImportSpec]): Seq[JenkinsBuildImportStatus] = lock.withLock {
    specStatuses.get(id).toSeq.flatMap(importStatus ⇒ importStatus.getBuildStatuses.map(_.snapshot))
  }

  def getJobImportStatus(id: Id[JenkinsImportSpec]): Option[JenkinsJobImportStatus] = lock.withLock {
    specStatuses.get(id).map(_.snapshot)
  }

  def buildExists(id: Id[JenkinsImportSpec], buildUrl: URI, buildNumber: Int) = lock.withLock {
    specStatuses(id).buildExists(buildUrl, buildNumber)
  }

  def buildStarted(id: Id[JenkinsImportSpec], buildUrl: URI) = lock.withLock {
    specStatuses(id).buildStarted(buildUrl)
  }

  def buildComplete(id: Id[JenkinsImportSpec], buildUrl: URI, batchIdOpt: Option[Id[Batch]]) = lock.withLock {
    specStatuses(id).buildComplete(buildUrl, batchIdOpt)
  }

  def buildErrored(id: Id[JenkinsImportSpec], buildUrl: URI, t: Throwable) = lock.withLock {
    specStatuses(id).buildErrored(buildUrl, t)
  }

  private class MutableJenkinsSpecImportStatus(specId: Id[JenkinsImportSpec], jobUrl: URI) {

    private var updatedAt: DateTime = clock.now

    private var state: JobImportState = JobImportState.NotStarted

    private var buildStatuses: Map[URI, MutableJenkinsBuildImportStatus] = Map()

    def started() = {
      updatedAt = clock.now
      state = JobImportState.InProgress
    }

    def complete() = {
      updatedAt = clock.now
      state = JobImportState.Complete
    }

    def errored(t: Throwable) = {
      updatedAt = clock.now
      state = JobImportState.Errored(t)
    }

    def getBuildStatuses = buildStatuses.values.toSeq

    def buildExists(buildUrl: URI, buildNumber: Int) = {
      buildStatuses += buildUrl -> new MutableJenkinsBuildImportStatus(buildUrl, buildNumber)
    }

    def buildStarted(buildUrl: URI) = {
      buildStatuses(buildUrl).buildStarted()
    }

    def buildComplete(buildUrl: URI, batchIdOpt: Option[Id[Batch]]) = {
      buildStatuses(buildUrl).buildComplete(batchIdOpt)
    }

    def buildErrored(buildUrl: URI, t: Throwable) = {
      buildStatuses(buildUrl).buildErrored(t)
    }

    def snapshot = JenkinsJobImportStatus(specId, updatedAt, state)

  }

  private class MutableJenkinsBuildImportStatus(val buildUrl: URI, val buildNumber: Int) {

    private var updatedAt: DateTime = clock.now

    private var state: BuildImportState = BuildImportState.NotStarted

    def buildStarted() {
      updatedAt = clock.now
      state = BuildImportState.InProgress
    }

    def buildComplete(batchIdOpt: Option[Id[Batch]]) {
      updatedAt = clock.now
      state = BuildImportState.Complete(batchIdOpt)
    }

    def buildErrored(t: Throwable) {
      updatedAt = clock.now
      state = BuildImportState.Errored(t)
    }

    def snapshot = JenkinsBuildImportStatus(buildUrl, buildNumber, updatedAt, state)

  }

}

case class JenkinsBuildImportStatus(buildUrl: URI, buildNumber: Int, updatedAt: DateTime, state: BuildImportState)

case class JenkinsJobImportStatus(specId: Id[JenkinsImportSpec], updatedAt: DateTime, state: JobImportState)

sealed trait BuildImportState

object BuildImportState {
  case class Complete(batchIdOpt: Option[Id[Batch]]) extends BuildImportState
  case object InProgress extends BuildImportState
  case class Errored(t: Throwable) extends BuildImportState
  case object NotStarted extends BuildImportState
}

sealed trait JobImportState

object JobImportState {
  case object Complete extends JobImportState
  case object InProgress extends JobImportState
  case class Errored(t: Throwable) extends JobImportState
  case object NotStarted extends JobImportState
}
