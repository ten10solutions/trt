package com.thetestpeople.trt.model.impl

import org.joda.time._
import com.github.tototoshi.slick.GenericJodaSupport
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.KeyedLocks
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.utils.LockUtils._
import javax.sql.DataSource
import java.net.URI
import scala.slick.util.CloseableIterator
import scala.slick.driver.H2Driver

trait SlickBatchDao extends BatchDao { this: SlickDao ⇒

  import driver.simple._
  import Database.dynamicSession
  import jodaSupport._
  import Mappers._
  import Tables._

  def getBatch(id: Id[Batch]): Option[EnrichedBatch] = {
    val join = batches
      .leftJoin(batchLogs).on(_.id === _.batchId)
      .leftJoin(ciBuilds).on(_._1.id === _.batchId)
      .leftJoin(batchComments).on(_._1._1.id === _.batchId)
    val query =
      for {
        (((batch, log), ciBuild), comment) ← join
        if batch.id === id
      } yield (batch, log.?, ciBuild.?, comment.?)
    query.firstOption.map {
      case (batch, logRowOpt, ciBuildOpt, commentOpt) ⇒
        EnrichedBatch(batch,
          logOpt = logRowOpt.map(_.log),
          importSpecIdOpt = ciBuildOpt.flatMap(_.importSpecIdOpt),
          commentOpt = commentOpt.map(_.text))
    }
  }

  def getBatches(jobOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean]): Seq[Batch] = {
    var query =
      jobOpt match {
        case Some(jobId) ⇒
          for {
            batch ← batches
            ciBuild ← ciBuilds
            if ciBuild.batchId === batch.id
            if ciBuild.jobId === jobId
          } yield batch
        case None ⇒
          batches
      }
    for (configuration ← configurationOpt)
      query = query.filter(_.configuration === configuration)
    for (result ← resultOpt)
      query = query.filter(_.passed === result)
    query.sortBy(_.executionTime.desc).run
  }

  def newBatch(batch: Batch, logOpt: Option[String]): Id[Batch] = {
    val batchId = (batches returning batches.map(_.id)).insert(batch)
    for (log ← logOpt)
      batchLogs.insert(BatchLogRow(batchId, removeNullChars(log)))
    batchId
  }

  def updateBatch(batch: Batch): Unit =
    batches.filter(_.id === batch.id).update(batch)

  def setBatchDuration(batchId: Id[Batch], durationOpt: Option[Duration]): Boolean =
    batches.filter(_.id === batchId).map(_.duration).update(durationOpt) > 0

  def deleteBatches(batchIds: Seq[Id[Batch]]): DeleteBatchResult =
    Cache.invalidate(configurationsCache, executionCountCache) {
      val (executionIds, testIds) = executions.filter(_.batchId inSet batchIds).map(e ⇒ (e.id, e.testId)).run.unzip
      ciBuilds.filter(_.batchId inSet batchIds).delete
      analyses.filter(_.testId inSet testIds).delete
      executionLogs.filter(_.executionId inSet executionIds).delete
      executionComments.filter(_.executionId inSet executionIds).delete
      executions.filter(_.id inSet executionIds).delete
      batchLogs.filter(_.batchId inSet batchIds).delete
      batchComments.filter(_.batchId inSet batchIds).delete
      batches.filter(_.id inSet batchIds).delete
      val deletedTestIds = deleteTestsThatHaveNoExecutions(testIds).toSet
      val remainingTestIds = testIds.filterNot(deletedTestIds.contains)
      DeleteBatchResult(remainingTestIds, executionIds)
    }

  /**
   * From amongst the given testIds, find those which have no executions, and delete them
   * @return the ids of the deleted tests
   */
  private def deleteTestsThatHaveNoExecutions(testIds: Seq[Id[Test]]): Seq[Id[Test]] = {
    val testsWithoutExecutionsQuery =
      for {
        (test, execution) ← tests leftJoin executions on (_.id === _.testId)
        if test.id inSet testIds
        if execution.id.?.isEmpty
      } yield test.id

    val testIdsToDelete = testsWithoutExecutionsQuery.run
    testCategories.filter(_.testId inSet testIdsToDelete).delete
    testComments.filter(_.testId inSet testIdsToDelete).delete
    tests.filter(_.id inSet testIdsToDelete).delete
    testIdsToDelete
  }

  def setBatchComment(id: Id[Batch], text: String): Unit =
    if (batchComments.filter(_.batchId === id).firstOption.isDefined)
      batchComments.filter(_.batchId === id).map(_.text).update(text)
    else
      batchComments.insert(BatchComment(id, text))

  def deleteBatchComment(id: Id[Batch]): Unit = batchComments.filter(_.batchId === id).delete

}