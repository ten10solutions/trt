package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.ExecutionLite

trait ExecutionAnalyser[T] {

  def executionGroup(executionGroup: ExecutionGroup)

  def finalise(): T

  def process(executions: Iterator[ExecutionLite]): T = {
    for (group ← new ExecutionGroupIterator(executions))
      executionGroup(group)
    finalise()
  }

}