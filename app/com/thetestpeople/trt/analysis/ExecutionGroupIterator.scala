package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.ExecutionLite

/**
 * Group of executions sharing the same configuration and testId
 */
case class ExecutionGroup(executions: List[ExecutionLite]) {

  def configuration = executions(0).configuration

}

/**
 * Iterates over successive groups of executions that share the same configuration and testId
 */
class ExecutionGroupIterator(executions: Iterator[ExecutionLite]) extends Iterator[ExecutionGroup] {

  private var remaining: Iterator[ExecutionLite] = executions

  def next(): ExecutionGroup = {
    val first = remaining.next()
    val (same, rest) = executions.span(e ⇒ e.configuration == first.configuration && e.testId == first.testId)
    remaining = rest
    ExecutionGroup(first :: same.toList)
  }

  def hasNext = remaining.hasNext

}