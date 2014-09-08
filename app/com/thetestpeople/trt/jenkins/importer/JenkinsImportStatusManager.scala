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

  private var specStatuses: Map[Id[JenkinsImportSpec], JenkinsSpecImportStatus] = Map()

  def importStarted(id: Id[JenkinsImportSpec]) = lock.withLock {
    specStatuses += id -> new JenkinsSpecImportStatus
  }

  def importComplete(id: Id[JenkinsImportSpec]) = lock.withLock {
    specStatuses(id).complete()
  }

  def importErrored(id: Id[JenkinsImportSpec]) = lock.withLock {
    specStatuses(id).errored()
  }

  def buildStarted(id: Id[JenkinsImportSpec], buildUrl: URI) = lock.withLock {
    specStatuses(id).buildStarted(buildUrl)
  }

  def buildComplete(id: Id[JenkinsImportSpec], buildUrl: URI, batchId: Id[Batch], numberOfExecutions: Int) = lock.withLock {
    specStatuses(id).buildComplete(buildUrl, batchId, numberOfExecutions)
  }

  def buildErrored(id: Id[JenkinsImportSpec], buildUrl: URI) = lock.withLock {
    specStatuses(id).buildErrored(buildUrl)
  }

  private class JenkinsSpecImportStatus() {

    private var updatedAt: DateTime = clock.now

    private var state: JobImportState = JobImportState.InProgress

    private var buildStatuses: Map[URI, JenkinsBuildImportStatus] = Map()

    def complete() = {
      updatedAt = clock.now
      state = JobImportState.Complete
    }

    def errored() = {
      updatedAt = clock.now
      state = JobImportState.Errored
    }

    def buildStarted(buildUrl: URI) = {
      buildStatuses += buildUrl -> new JenkinsBuildImportStatus
    }

    def buildComplete(buildUrl: URI, batchId: Id[Batch], numberOfExecutions: Int) = {
      buildStatuses(buildUrl).buildComplete(batchId, numberOfExecutions)
    }

    def buildErrored(buildUrl: URI) = {
      buildStatuses(buildUrl).buildErrored()
    }

  }

  class JenkinsBuildImportStatus {

    private var updatedAt: DateTime = clock.now

    private var state: BuildImportState = BuildImportState.InProgress

    def buildComplete(batchId: Id[Batch], numberOfExecutions: Int) {
      updatedAt = clock.now
      state = BuildImportState.Complete(batchId, numberOfExecutions)
    }

    def buildErrored() {
      updatedAt = clock.now
      state = BuildImportState.Errored
    }

  }

}

sealed trait BuildImportState
object BuildImportState {
  case class Complete(batchId: Id[Batch], numberOfExecutions: Int) extends BuildImportState
  case object InProgress extends BuildImportState
  case object Errored extends BuildImportState
}

sealed trait JobImportState
object JobImportState {
  case object Complete extends JobImportState
  case object InProgress extends JobImportState
  case object Errored extends JobImportState
}
