package com.thetestpeople.trt.model

case class BatchComment(batchId: Id[Batch], text: String)

case class ExecutionComment(executionId: Id[Execution], text: String)

case class TestComment(testId: Id[Test], text: String)