package com.thetestpeople.trt.importer

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
class CiImportStatusManager(clock: Clock) {

  private val lock: Lock = new ReentrantLock

  private var specStatuses: Map[Id[CiImportSpec], MutableCiSpecImportStatus] = Map()

  def importStarted(id: Id[CiImportSpec], jobUrl: URI) = lock.withLock {
    specStatuses += id -> new MutableCiSpecImportStatus(id, jobUrl)
    specStatuses(id).started()
  }

  def importComplete(id: Id[CiImportSpec]) = lock.withLock {
    specStatuses(id).complete()
  }

  def importErrored(id: Id[CiImportSpec], t: Throwable) = lock.withLock {
    specStatuses(id).errored(t)
  }

  def getBuildImportStatuses(id: Id[CiImportSpec]): Seq[CiBuildImportStatus] = lock.withLock {
    specStatuses.get(id).toSeq.flatMap(importStatus ⇒ importStatus.getBuildStatuses.map(_.snapshot))
  }

  def getJobImportStatus(id: Id[CiImportSpec]): Option[CiJobImportStatus] = lock.withLock {
    specStatuses.get(id).map(_.snapshot)
  }

  def buildExists(id: Id[CiImportSpec], buildUrl: URI, buildNumberOpt: Option[Int]) = lock.withLock {
    specStatuses(id).buildExists(buildUrl, buildNumberOpt)
  }

  def buildStarted(id: Id[CiImportSpec], buildUrl: URI) = lock.withLock {
    specStatuses(id).buildStarted(buildUrl)
  }

  def buildComplete(id: Id[CiImportSpec], buildUrl: URI, batchIdOpt: Option[Id[Batch]]) = lock.withLock {
    specStatuses(id).buildComplete(buildUrl, batchIdOpt)
  }

  def buildErrored(id: Id[CiImportSpec], buildUrl: URI, t: Throwable) = lock.withLock {
    specStatuses(id).buildErrored(buildUrl, t)
  }

  private class MutableCiSpecImportStatus(specId: Id[CiImportSpec], jobUrl: URI) {

    private var updatedAt: DateTime = clock.now

    private var state: JobImportState = JobImportState.NotStarted

    private var buildStatuses: Map[URI, MutableCiBuildImportStatus] = Map()

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

    def buildExists(buildUrl: URI, buildNumberOpt: Option[Int]) = {
      buildStatuses += buildUrl -> new MutableCiBuildImportStatus(buildUrl, buildNumberOpt)
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

    def snapshot = CiJobImportStatus(specId, updatedAt, state)

  }

  private class MutableCiBuildImportStatus(val buildUrl: URI, val buildNumberOpt: Option[Int]) {

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

    def snapshot = CiBuildImportStatus(buildUrl, buildNumberOpt, updatedAt, state)

  }

}

case class CiBuildImportStatus(buildUrl: URI, buildNumberOpt: Option[Int], updatedAt: DateTime, state: BuildImportState)

case class CiJobImportStatus(specId: Id[CiImportSpec], updatedAt: DateTime, state: JobImportState)

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
