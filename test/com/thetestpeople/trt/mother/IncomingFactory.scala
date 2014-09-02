package com.thetestpeople.trt.mother

import org.joda.time.DateTime
import org.joda.time.Duration
import com.thetestpeople.trt.service.Incoming
import com.thetestpeople.trt.service.Incoming._
import com.thetestpeople.trt.model.impl.DummyData
import com.thetestpeople.trt.model.QualifiedName
import com.thetestpeople.trt.model.Configuration
import java.net.URI

object IncomingFactory {

  def test(qualifiedName: QualifiedName): Incoming.Test = Incoming.Test(qualifiedName.name, qualifiedName.groupOpt)

  def test(
    name: String = DummyData.TestName,
    groupOpt: Option[String] = Some(DummyData.Group)): Incoming.Test = Incoming.Test(name, groupOpt)

  def execution(
    test: Test = IncomingFactory.test(),
    passed: Boolean = true,
    summaryOpt: Option[String] = Some(DummyData.Summary),
    logOpt: Option[String] = Some(DummyData.Log),
    executionTimeOpt: Option[DateTime] = Some(DummyData.ExecutionTime),
    durationOpt: Option[Duration] = Some(DummyData.Duration),
    configurationOpt: Option[Configuration] = None): Incoming.Execution =
    Incoming.Execution(
      test = test,
      passed = passed,
      summaryOpt = summaryOpt,
      logOpt = logOpt,
      executionTimeOpt = executionTimeOpt,
      durationOpt = durationOpt,
      configurationOpt = configurationOpt)

  def batch(
    urlOpt: Option[URI] = Some(DummyData.BuildUrl),
    executionTimeOpt: Option[DateTime] = Some(DummyData.ExecutionTime),
    nameOpt: Option[String] = Some(DummyData.BatchName),
    logOpt: Option[String] = Some(DummyData.Log),
    durationOpt: Option[Duration] = Some(DummyData.Duration),
    executions: List[Incoming.Execution] = List(IncomingFactory.execution())): Incoming.Batch =
    Incoming.Batch(
      urlOpt = urlOpt,
      nameOpt = nameOpt,
      logOpt = logOpt,
      executionTimeOpt = executionTimeOpt,
      durationOpt = durationOpt,
      executions = executions)

  private def opt[T](t: T): Option[T] = if (t == null) None else Some(t)

}