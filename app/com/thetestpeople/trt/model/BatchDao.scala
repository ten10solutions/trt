package com.thetestpeople.trt.model

import com.thetestpeople.trt.model.jenkins.CiJob
import org.joda.time.Duration

trait BatchDao {

  def getBatch(id: Id[Batch]): Option[EnrichedBatch]

  /**
   * Return batches, ordered most recent first
   *
   * @param jobOpt -- if Some(job), then only return batches that were imported from the given job.
   * @param configurationOpt -- if Some(configuration), then only return batches that are associated with the given configuration
   * Otherwise, return all batches.
   */
  def getBatches(jobOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean] = None): Seq[Batch]

  /**
   * Add a record for a new batch (the existing ID is ignored)
   *
   * @return ID of the newly added batch.
   */
  def newBatch(batch: Batch, logOpt: Option[String] = None): Id[Batch]

  def setBatchDuration(batchId: Id[Batch], durationOpt: Option[Duration]): Boolean

  def updateBatch(batch: Batch)

  /**
   * Delete the given batches and any associated data (executions, Jenkins import records, etc).
   *
   * This will also delete any tests that end up with no executions as a result of deleting the given batches.
   *
   * @return IDs of tests that haven't been deleted, but have had an associated execution deleted.
   */
  def deleteBatches(batchIds: Seq[Id[Batch]]): DeleteBatchResult

  def setBatchComment(id: Id[Batch], text: String)

  def deleteBatchComment(id: Id[Batch])

}


/**
 * @param remainingTestIds -- ids of tests that had executions in the deleted batch, but weren't themselves deleted
 */
case class DeleteBatchResult(remainingTestIds: Seq[Id[Test]], deletedExecutionIds: Seq[Id[Execution]])