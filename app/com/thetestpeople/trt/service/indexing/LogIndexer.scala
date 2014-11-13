package com.thetestpeople.trt.service.indexing

import com.thetestpeople.trt.model.EnrichedExecution
import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.Execution

trait LogIndexer {

  def addExecutions(executions: Seq[EnrichedExecution])

  def searchExecutions(query: String, startingFrom: Int = 0, limit: Int = Integer.MAX_VALUE): SearchResult

  def deleteExecutions(ids: Seq[Id[Execution]])

  def deleteAll()

}

case class SearchResult(hits: Seq[ExecutionHit], total: Int)

case class ExecutionHit(executionId: Id[Execution], matchingFragment: String)