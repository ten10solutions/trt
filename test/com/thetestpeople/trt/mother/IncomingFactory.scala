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

  def test(qualifiedName: QualifiedName): Incoming.Test = 
    Incoming.Test(qualifiedName.name, qualifiedName.groupOpt)

  def test(
    name: String = DummyData.TestName,
    groupOpt: Option[String] = Some(DummyData.Group),
    categories: Seq[String] = Seq()): Incoming.Test = Incoming.Test(name, groupOpt, categories)

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
    complete: Boolean = true,
    urlOpt: Option[URI] = Some(DummyData.BuildUrl),
    executionTimeOpt: Option[DateTime] = Some(DummyData.ExecutionTime),
    nameOpt: Option[String] = Some(DummyData.BatchName),
    logOpt: Option[String] = Some(DummyData.Log),
    durationOpt: Option[Duration] = Some(DummyData.Duration),
    executions: Seq[Incoming.Execution] = Seq(IncomingFactory.execution()),
    configurationOpt: Option[Configuration] = None): Incoming.Batch =
    Incoming.Batch(
      complete = complete,
      urlOpt = urlOpt,
      nameOpt = nameOpt,
      logOpt = logOpt,
      executionTimeOpt = executionTimeOpt,
      durationOpt = durationOpt,
      executions = executions,
      configurationOpt = configurationOpt)

  private def opt[T](t: T): Option[T] = if (t == null) None else Some(t)

}