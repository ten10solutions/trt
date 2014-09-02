package com.thetestpeople.trt.analysis

import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.impl.DummyData
import com.github.nscala_time.time.Imports._

@RunWith(classOf[JUnitRunner])
class ExecutionGroupIteratorTest extends FlatSpec with Matchers {

  "Execution group iterator" should "group executions with the same configuration and test id" in {
    val execution1 = execution(DummyData.Configuration1, Id[Test](1))
    val execution2 = execution(DummyData.Configuration2, Id[Test](1))
    val execution3 = execution(DummyData.Configuration2, Id[Test](2))
    val execution4 = execution(DummyData.Configuration2, Id[Test](2))

    val iterator = executionGroupIterator(
      execution1,
      execution2,
      execution3,
      execution4)

    iterator.toList should equal(List(
      executionGroup(execution1),
      executionGroup(execution2),
      executionGroup(execution3, execution4)))
  }

  private def executionGroup(executions: ExecutionLite*): ExecutionGroup = ExecutionGroup(executions.toList)
  
  private def executionGroupIterator(executions: ExecutionLite*): ExecutionGroupIterator =
    new ExecutionGroupIterator(executions.iterator)

  private def execution(configuration: Configuration, testId: Id[Test]): ExecutionLite =
    ExecutionLite(
      configuration = configuration,
      testId = testId,
      executionTime = DummyData.ExecutionTime,
      passed = true)
}