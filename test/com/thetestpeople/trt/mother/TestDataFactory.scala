package com.thetestpeople.trt.mother

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.model.impl._
import org.joda.time._
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

object TestDataFactory {

  private val testNameCounter = new AtomicInteger(1)

  def test(qualifiedName: QualifiedName): Test = test(qualifiedName.name, qualifiedName.groupOpt)

  def test(
    name: String = "test" + testNameCounter.getAndIncrement(),
    groupOpt: Option[String] = Some(DummyData.Group),
    deleted: Boolean = false): Test =
    Test(name = name, groupOpt = groupOpt, deleted = deleted)

  def batch(
    urlOpt: Option[URI] = Some(DummyData.BuildUrl),
    executionTime: DateTime = DummyData.ExecutionTime,
    durationOpt: Option[Duration] = Some(DummyData.Duration),
    nameOpt: Option[String] = Some(DummyData.BatchName),
    passed: Boolean = true,
    totalCount: Int = 10,
    passCount: Int = 5,
    failCount: Int = 5,
    configurationOpt: Option[Configuration] = None): Batch =
    Batch(
      urlOpt = urlOpt,
      executionTime = executionTime,
      durationOpt = durationOpt,
      nameOpt = nameOpt,
      passed = passed,
      totalCount = totalCount,
      passCount = passCount,
      failCount = failCount,
      configurationOpt = configurationOpt)

  def execution(
    batchId: Id[Batch],
    testId: Id[Test],
    executionTime: DateTime = DummyData.ExecutionTime,
    durationOpt: Option[Duration] = Some(DummyData.Duration),
    summaryOpt: Option[String] = Some(DummyData.Summary),
    passed: Boolean = true,
    configuration: Configuration = Configuration.Default): Execution =
    Execution(
      batchId = batchId,
      testId = testId,
      executionTime = executionTime,
      durationOpt = durationOpt,
      summaryOpt = summaryOpt,
      passed = passed,
      configuration = configuration)

  def analysis(
    testId: Id[Test],
    status: TestStatus = TestStatus.Healthy,
    configuration: Configuration = Configuration.Default,
    weather: Double = DummyData.Weather,
    consecutiveFailures: Int = DummyData.ConsecutiveFailures,
    failingSinceOpt: Option[DateTime] = None,
    lastPassedExecutionIdOpt: Option[Id[Execution]] = None,
    lastPassedTimeOpt: Option[DateTime] = None,
    lastFailedExecutionIdOpt: Option[Id[Execution]] = None,
    lastFailedTimeOpt: Option[DateTime] = None,
    whenAnalysed: DateTime = DummyData.WhenAnalysed,
    medianDurationOpt: Option[Duration] = Some(DummyData.Duration)): Analysis =
    Analysis(
      testId = testId,
      configuration = configuration,
      status = status,
      weather = weather,
      consecutiveFailures = consecutiveFailures,
      failingSinceOpt = failingSinceOpt,
      lastPassedExecutionIdOpt = lastPassedExecutionIdOpt,
      lastPassedTimeOpt = lastPassedTimeOpt,
      lastFailedExecutionIdOpt = lastFailedExecutionIdOpt,
      lastFailedTimeOpt = lastFailedTimeOpt,
      whenAnalysed = whenAnalysed,
      medianDurationOpt = medianDurationOpt)

  def ciImportSpec(
    jobUrl: URI = DummyData.JobUrl,
    ciType: CiType = CiType.Jenkins,
    pollingInterval: Duration = DummyData.PollingInterval,
    importConsoleLog: Boolean = true,
    lastCheckedOpt: Option[DateTime] = None,
    configurationOpt: Option[Configuration] = None): CiImportSpec =
    CiImportSpec(
      ciType = ciType,
      jobUrl = jobUrl,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      lastCheckedOpt = lastCheckedOpt,
      configurationOpt = configurationOpt)

  def ciJob(
    url: URI = DummyData.JobUrl,
    name: String = DummyData.JobName) =
    CiJob(
      url = url,
      name = name)

  def jenkinsBuild(
    batchId: Id[Batch],
    jobId: Id[CiJob],
    importTime: DateTime = DummyData.ImportTime,
    buildUrl: URI = DummyData.BuildUrl,
    buildNumberOpt: Option[Int] = Some(DummyData.BuildNumber),
    buildNameOpt: Option[String] = Some(DummyData.BatchName),
    importSpecIdOpt: Option[Id[CiImportSpec]] = None) =
    CiBuild(batchId, importTime, buildUrl, buildNumberOpt, buildNameOpt, jobId, importSpecIdOpt)

 def testCategory(testId: Id[Test], category: String = DummyData.Category, isUserCategory: Boolean = false) : TestCategory = 
   TestCategory(testId, category, isUserCategory)
    
}