package com.thetestpeople.trt.model.impl

import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }
import com.thetestpeople.trt.utils.UriUtils._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.joda.time.DateTime
import org.joda.time.Duration
import java.net.URI

abstract class AbstractDaoTest extends FlatSpec with Matchers with ExecutionDaoTest with JenkinsDaoTest {

  protected def createDao: Dao

  protected def transaction[T](p: Dao ⇒ T): T = {
    val dao = createDao
    dao.transaction {
      p(dao)
    }
  }

  "Recording new test" should "persist all the data in the test" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test(
      name = DummyData.TestName,
      groupOpt = Some(DummyData.Group)))

    val Seq(test) = dao.getTestsById(List(testId))
    test.name should equal(DummyData.TestName)
    test.groupOpt should equal(Some(DummyData.Group))
  }

  "Recording an already existing test" should "have no effect" in transaction { dao ⇒
    val test = F.test(name = DummyData.TestName, groupOpt = Some(DummyData.Group))

    val testId = dao.ensureTestIsRecorded(test)
    val testIdAgain = dao.ensureTestIsRecorded(test)

    testIdAgain should equal(testId)
    dao.getTestIds() should equal(List(testId))
  }

  "Inserting a new batch" should "persist all the batch data" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch(
      urlOpt = Some(DummyData.BuildUrl),
      executionTime = DummyData.ExecutionTime,
      durationOpt = Some(DummyData.Duration),
      nameOpt = Some(DummyData.BatchName),
      passed = true,
      totalCount = DummyData.TotalCount,
      passCount = DummyData.PassCount,
      failCount = DummyData.FailCount,
      configurationOpt = Some(DummyData.Configuration1)),
      logOpt = Some(DummyData.Log))

    val Some(BatchAndLog(batch, Some(log))) = dao.getBatch(batchId)
    batch.urlOpt should equal(Some(DummyData.BuildUrl))
    batch.executionTime should equal(DummyData.ExecutionTime)
    batch.durationOpt should equal(Some(DummyData.Duration))
    batch.nameOpt should equal(Some(DummyData.BatchName))
    batch.passed should be(true)
    batch.totalCount should be(DummyData.TotalCount)
    batch.passCount should be(DummyData.PassCount)
    batch.failCount should be(DummyData.FailCount)
    batch.configurationOpt should equal(Some(DummyData.Configuration1))
    log should equal(DummyData.Log)
  }

  "Inserting a new batch" should "handle absent data" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch(
      urlOpt = None,
      durationOpt = None,
      nameOpt = None),
      logOpt = None)

    val Some(BatchAndLog(batch, None)) = dao.getBatch(batchId)
    batch.urlOpt should equal(None)
    batch.durationOpt should equal(None)
    batch.nameOpt should equal(None)
  }
  "Getting all analysed tests" should "work" in transaction { dao ⇒
    val dao = createDao
    def addTest() = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val testId1 = addTest()
    val testId2 = addTest()
    val testId3 = addTest()

    val tests = dao.getAnalysedTests().map(_.test)

    tests.map(_.id) should contain theSameElementsAs (List(testId1, testId2, testId3))
  }

  "Getting tests" should "allow you to filter by group" in transaction { dao ⇒
    val dao = createDao
    def addTest(group: String) = {
      val testId = dao.ensureTestIsRecorded(F.test(groupOpt = Some(group)))
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val testId1 = addTest("Group 1")
    val testId2 = addTest("Group 1")
    val testId3 = addTest("Group 2")

    val tests = dao.getAnalysedTests(groupOpt = Some("Group 1")).map(_.test)

    tests.map(_.id) should contain theSameElementsAs (List(testId1, testId2))
  }

  "Getting tests" should "allow you to filter by status" in transaction { dao ⇒
    val dao = createDao
    def addTest(status: TestStatus) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, status))
      testId
    }
    val testId1 = addTest(TestStatus.Pass)
    val testId2 = addTest(TestStatus.Warn)
    val testId3 = addTest(TestStatus.Warn)
    val testId4 = addTest(TestStatus.Fail)
    val testId5 = addTest(TestStatus.Fail)
    val testId6 = addTest(TestStatus.Fail)

    def getTestIds(status: TestStatus) = dao.getAnalysedTests(testStatusOpt = Some(status)).map(_.test.id)
    getTestIds(TestStatus.Pass) should contain theSameElementsAs (List(testId1))
    getTestIds(TestStatus.Warn) should contain theSameElementsAs (List(testId2, testId3))
    getTestIds(TestStatus.Fail) should contain theSameElementsAs (List(testId4, testId5, testId6))
  }

  "Getting tests" should "let you limit the number of results" in transaction { dao ⇒
    val dao = createDao
    def addTest() = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val testId0 = addTest()
    val testId1 = addTest()
    val testId2 = addTest()
    val testId3 = addTest()
    val testId4 = addTest()
    val testId5 = addTest()

    dao.getAnalysedTests(startingFrom = 3, limitOpt = Some(2)).map(_.test.id) should contain theSameElementsAs (List(testId3, testId4))
    dao.getAnalysedTests(startingFrom = 4, limitOpt = None).map(_.test.id) should contain theSameElementsAs (List(testId4, testId5))
  }

  "Getting tests" should "return results ordered by group, then name" in transaction { dao ⇒
    def addTest(name: String, group: String) = {
      val testId = dao.ensureTestIsRecorded(F.test(name, Some(group)))
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val testId_C1 = addTest(name = "1", group = "C")
    val testId_A0 = addTest(name = "0", group = "A")
    val testId_A1 = addTest(name = "1", group = "A")
    val testId_B0 = addTest(name = "0", group = "B")
    val testId_C0 = addTest(name = "0", group = "C")
    val testId_B1 = addTest(name = "1", group = "B")

    dao.getAnalysedTests().map(_.test.id) should contain theSameElementsAs (List(
      testId_A0, testId_A1, testId_B0, testId_B1, testId_C0, testId_C1))
  }

  "Getting tests by ID" should "work" in transaction { dao ⇒
    val dao = createDao
    val testId1 = dao.ensureTestIsRecorded(F.test())
    val testId2 = dao.ensureTestIsRecorded(F.test())
    val testId3 = dao.ensureTestIsRecorded(F.test())

    val Seq(test1) = dao.getTestsById(List(testId1))
    test1.id should equal(testId1)
  }

  "Getting test counts" should "totally work" in transaction { dao ⇒
    val dao = createDao
    def addTest(status: TestStatus) {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, status))
    }
    addTest(TestStatus.Pass)
    addTest(TestStatus.Warn)
    addTest(TestStatus.Warn)
    addTest(TestStatus.Fail)
    addTest(TestStatus.Fail)
    addTest(TestStatus.Fail)

    val testCounts = dao.getTestCounts()

    testCounts should equal(TestCounts(passed = 1, warning = 2, failed = 3))
  }

  "Getting test counts by configuration" should "work" in transaction { dao ⇒
    val dao = createDao
    def addTest(configuration: Configuration, status: TestStatus) {
      val testId = dao.ensureTestIsRecorded(F.test())
      val batchId = dao.newBatch(F.batch())
      dao.newExecution(F.execution(batchId, testId, configuration = configuration))
      dao.upsertAnalysis(F.analysis(testId, status, configuration))
    }
    addTest(DummyData.Configuration1, TestStatus.Pass)
    addTest(DummyData.Configuration1, TestStatus.Warn)
    addTest(DummyData.Configuration1, TestStatus.Warn)
    addTest(DummyData.Configuration1, TestStatus.Fail)
    addTest(DummyData.Configuration1, TestStatus.Fail)
    addTest(DummyData.Configuration1, TestStatus.Fail)
    addTest(DummyData.Configuration2, TestStatus.Pass)
    addTest(DummyData.Configuration2, TestStatus.Warn)
    addTest(DummyData.Configuration2, TestStatus.Fail)

    val testCountsByConfiguration = dao.getTestCountsByConfiguration()

    testCountsByConfiguration should equal(Map(
      DummyData.Configuration1 -> TestCounts(passed = 1, warning = 2, failed = 3),
      DummyData.Configuration2 -> TestCounts(passed = 1, warning = 1, failed = 1)))
  }

  "Getting test counts" should "allow filtering by group" in transaction { dao ⇒
    val dao = createDao
    def addTest(group: String, status: TestStatus) {
      val testId = dao.ensureTestIsRecorded(F.test(groupOpt = Some(group)))
      dao.upsertAnalysis(F.analysis(testId, status))
    }
    addTest("group1", TestStatus.Pass)
    addTest("group1", TestStatus.Warn)
    addTest("group1", TestStatus.Warn)
    addTest("group1", TestStatus.Fail)
    addTest("group1", TestStatus.Fail)
    addTest("group1", TestStatus.Fail)
    addTest("group2", TestStatus.Pass)

    val testCounts = dao.getTestCounts(groupOpt = Some("group1"))

    testCounts should equal(TestCounts(passed = 1, warning = 2, failed = 3))

  }

  "Getting test counts" should "tolerate unanalysed tests" in transaction { dao ⇒
    val dao = createDao
    val testId = dao.ensureTestIsRecorded(F.test())
    val testCounts = dao.getTestCounts()
    testCounts should equal(TestCounts(passed = 0, warning = 0, failed = 0))
  }

  "Ensuring a test is recorded" should "record the test if it doesn't already exist" in transaction { dao ⇒
    val test = F.test(name = DummyData.TestName, groupOpt = Some(DummyData.Group))

    val testId = dao.ensureTestIsRecorded(test)

    val Seq(testAgain) = dao.getTestsById(List(testId))
    testAgain.qualifiedName should equal(test.qualifiedName)
  }

  it should "record a test without a group" in transaction { dao ⇒
    val test = F.test(name = DummyData.TestName, groupOpt = None)

    val testId = dao.ensureTestIsRecorded(test)

    val Seq(testAgain) = dao.getTestsById(List(testId))
    testAgain.qualifiedName should equal(test.qualifiedName)
  }

  it should "record a new test even if there exists a test with the same name, but different group" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test(name = DummyData.TestName, groupOpt = Some("group1")))
    val test = F.test(name = DummyData.TestName, groupOpt = Some("group2"))

    val testId2 = dao.ensureTestIsRecorded(test)

    val Seq(testAgain) = dao.getTestsById(List(testId2))
    testAgain.qualifiedName should equal(test.qualifiedName)
    dao.getTestIds() should contain theSameElementsAs (List(testId1, testId2))
  }

  it should "not record a new test if it has already been recorded" in transaction { dao ⇒
    val test = F.test(name = DummyData.TestName, groupOpt = Some("group2"))
    val testId = dao.ensureTestIsRecorded(test)

    val testIdAgain = dao.ensureTestIsRecorded(test)

    testIdAgain should equal(testId)
    dao.getTestIds() should contain theSameElementsAs (List(testId))
  }

  "Getting a test by id" should "not bring anything back if no test with that id" in transaction { dao ⇒
    val Some(bogusId) = Id.parse[Test]("123")

    dao.getTestAndAnalysis(bogusId) should equal(None)
  }

  "Getting batches" should "return them all most recent batch first" in transaction { dao ⇒
    val batchId1 = dao.newBatch(F.batch(executionTime = 1.day.ago))
    val batchId3 = dao.newBatch(F.batch(executionTime = 3.days.ago))
    val batchId2 = dao.newBatch(F.batch(executionTime = 2.days.ago))

    val batches = dao.getBatches()

    batches.map(_.id) should equal(List(batchId1, batchId2, batchId3))
  }

  "Getting batches" should "let you filter by Jenkins job" in transaction { dao ⇒
    def addBatchAssociatedWithJob(jobId: Id[JenkinsJob], buildUrl: URI) = {
      val batchId = dao.newBatch(F.batch())
      dao.newJenkinsBuild(F.jenkinsBuild(batchId, jobId, buildUrl = buildUrl))
      batchId
    }
    val jobId1 = dao.ensureJenkinsJob(F.jenkinsJob(url = DummyData.JobUrl))
    val jobId2 = dao.ensureJenkinsJob(F.jenkinsJob(url = DummyData.JobUrl2))
    val batchId1 = addBatchAssociatedWithJob(jobId1, DummyData.BuildUrl)
    val batchId2 = addBatchAssociatedWithJob(jobId2, DummyData.BuildUrl2)

    dao.getBatches(jobOpt = Some(jobId1)).map(_.id) should equal(List(batchId1))
    dao.getBatches(jobOpt = Some(jobId2)).map(_.id) should equal(List(batchId2))
    dao.getBatches(jobOpt = None).map(_.id) should contain theSameElementsAs (List(batchId1, batchId2))
  }

  "Getting batches" should "let you filter by configuration" in transaction { dao ⇒
    val batchId1 = dao.newBatch(F.batch(configurationOpt = Some(DummyData.Configuration1)))
    val batchId2 = dao.newBatch(F.batch(configurationOpt = Some(DummyData.Configuration2)))

    val Seq(batch) = dao.getBatches(configurationOpt = Some(DummyData.Configuration1))

    batch.id should equal(batchId1)
  }

  "Inserting and updating a new analysis" should "persist all the analysis data" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId1 = dao.newBatch(F.batch())
    val batchId2 = dao.newBatch(F.batch())
    val passedExecutionTime = 1.day.ago
    val passedExecutionId = dao.newExecution(F.execution(batchId1, testId, passed = true, executionTime = passedExecutionTime))
    val failedExecutionTime = 2.days.ago
    val failedExecutionId = dao.newExecution(F.execution(batchId2, testId, passed = false, executionTime = failedExecutionTime))

    dao.upsertAnalysis(F.analysis(
      testId = testId,
      status = TestStatus.Pass,
      weather = DummyData.Weather,
      consecutiveFailures = DummyData.ConsecutiveFailures,
      failingSinceOpt = Some(failedExecutionTime),
      lastPassedExecutionIdOpt = Some(passedExecutionId),
      lastPassedTimeOpt = Some(passedExecutionTime),
      lastFailedExecutionIdOpt = Some(failedExecutionId),
      lastFailedTimeOpt = Some(failedExecutionTime),
      whenAnalysed = DummyData.WhenAnalysed))

    val Some(TestAndAnalysis(_, Some(analysis))) = dao.getTestAndAnalysis(testId)
    analysis.testId should equal(testId)
    analysis.status should equal(TestStatus.Pass)
    analysis.weather should equal(DummyData.Weather)
    analysis.consecutiveFailures should equal(DummyData.ConsecutiveFailures)
    analysis.lastExecutionTime should equal(passedExecutionTime)
    analysis.lastExecutionId should equal(passedExecutionId)
    analysis.lastPassedTimeOpt should equal(Some(passedExecutionTime))
    analysis.lastPassedExecutionIdOpt should equal(Some(passedExecutionId))
    analysis.lastFailedTimeOpt should equal(Some(failedExecutionTime))
    analysis.lastFailedExecutionIdOpt should equal(Some(failedExecutionId))
    analysis.whenAnalysed should equal(DummyData.WhenAnalysed)

    dao.upsertAnalysis(F.analysis(
      testId = testId,
      status = TestStatus.Warn,
      weather = DummyData.Weather,
      consecutiveFailures = DummyData.ConsecutiveFailures,
      failingSinceOpt = None,
      lastPassedExecutionIdOpt = None,
      lastPassedTimeOpt = None,
      lastFailedExecutionIdOpt = None,
      lastFailedTimeOpt = None,
      whenAnalysed = DummyData.WhenAnalysed))

    val Some(TestAndAnalysis(_, Some(analysis2))) = dao.getTestAndAnalysis(testId)
    analysis2.testId should equal(testId)
    analysis2.status should equal(TestStatus.Warn)
    analysis2.weather should equal(DummyData.Weather)
    analysis2.consecutiveFailures should equal(DummyData.ConsecutiveFailures)
    analysis2.lastPassedTimeOpt should equal(None)
    analysis2.lastPassedExecutionIdOpt should equal(None)
    analysis2.lastFailedTimeOpt should equal(None)
    analysis2.lastFailedExecutionIdOpt should equal(None)
    analysis2.whenAnalysed should equal(DummyData.WhenAnalysed)

  }

  "Inserting and updating a new analysis" should "handle absent data" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())

    dao.upsertAnalysis(F.analysis(
      testId = testId,
      failingSinceOpt = None,
      lastPassedExecutionIdOpt = None,
      lastPassedTimeOpt = None,
      lastFailedExecutionIdOpt = None,
      lastFailedTimeOpt = None))

    val Some(TestAndAnalysis(_, Some(analysis))) = dao.getTestAndAnalysis(testId)
    analysis.lastPassedTimeOpt should equal(None)
    analysis.lastPassedExecutionIdOpt should equal(None)
    analysis.lastPassedExecutionIdOpt should equal(None)
    analysis.lastFailedTimeOpt should equal(None)
    analysis.lastFailedExecutionIdOpt should equal(None)

  }

  "Deleting a batch" should "delete associated data" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch(), logOpt = Some(DummyData.Log))
    val testId = dao.ensureTestIsRecorded(F.test())
    val executionId = dao.newExecution(F.execution(batchId, testId), logOpt = Some(DummyData.Log))
    val jobId = dao.ensureJenkinsJob(F.jenkinsJob())
    dao.newJenkinsBuild(F.jenkinsBuild(batchId, jobId = jobId, buildUrl = DummyData.BuildUrl))
    dao.upsertAnalysis(F.analysis(testId, lastPassedExecutionIdOpt = Some(executionId)))

    dao.deleteBatches(List(batchId))

    dao.getBatch(batchId) should equal(None)
    dao.getEnrichedExecutionsInBatch(batchId) should equal(List())
    dao.getJenkinsBuild(DummyData.BuildUrl) should equal(None)
  }

  "Deleting a batch" should "delete tests if they no longer have associated executions" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    val testId2 = dao.ensureTestIsRecorded(F.test())
    val testId3 = dao.ensureTestIsRecorded(F.test())
    val batchId1 = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId1, testId1))
    dao.newExecution(F.execution(batchId1, testId2))
    val batchId2 = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId2, testId1))
    val batchId3 = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId3, testId3))

    val Seq(affectedTestId) = dao.deleteBatches(List(batchId1))

    affectedTestId should be(testId1) // It hasn't been deleted, but it has had some executions removed
    dao.getTestAndAnalysis(testId2) should be(None) // It has had all its executions removed, so it has been removed too
  }

  "Inserting then updating two analyses" should "not cause a primary key error" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    val testId2 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId1))
    dao.upsertAnalysis(F.analysis(testId2))
    dao.upsertAnalysis(F.analysis(testId1))
    dao.upsertAnalysis(F.analysis(testId2))
  }

  "Inserting and retrieving system configuration" should "persist all the data" in transaction { dao ⇒
    val systemConfiguration = SystemConfiguration(
      failureDurationThreshold = 13.hours,
      failureCountThreshold = 7,
      passDurationThreshold = 9.hours,
      passCountThreshold = 13)
    dao.updateSystemConfiguration(systemConfiguration)
    dao.getSystemConfiguration() should equal(systemConfiguration)
  }

  "Deleting a test" should "mark it as deleted" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test(deleted = false))
    dao.markTestsAsDeleted(Seq(testId))
    val Seq(test) = dao.getTestsById(List(testId))
    test.deleted should be(true)
  }

  "Deleting a test" should "only mark that test as deleted" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test(deleted = false))
    val testId2 = dao.ensureTestIsRecorded(F.test(deleted = false))
    dao.markTestsAsDeleted(Seq(testId1))
    val Seq(test) = dao.getTestsById(List(testId2))
    test.deleted should be(false)
  }

  "Undeleting a test" should "work" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test(deleted = true))
    dao.markTestsAsDeleted(Seq(testId), deleted = false)
    val Seq(test) = dao.getTestsById(List(testId))
    test.deleted should be(false)
  }

  "Getting all configurations" should "work" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration1))
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration2))

    dao.getConfigurations() should contain theSameElementsAs (List(DummyData.Configuration1, DummyData.Configuration2))
  }

  "Getting all configurations for a test" should "work" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration1))
    dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration2))

    dao.getConfigurations(testId) should contain theSameElementsAs (List(DummyData.Configuration1, DummyData.Configuration2))
  }

  "Getting test names" should "find exact matches" in transaction { dao ⇒
    dao.ensureTestIsRecorded(F.test(name = "a"))
    dao.ensureTestIsRecorded(F.test(name = "b"))
    dao.ensureTestIsRecorded(F.test(name = "ab"))
    dao.ensureTestIsRecorded(F.test(name = "ba"))

    dao.getTestNames("a") should equal(Seq("a"))
  }

  it should "find matches with wildcards" in transaction { dao ⇒
    dao.ensureTestIsRecorded(F.test(name = "testsomething"))
    dao.ensureTestIsRecorded(F.test(name = "sometest"))
    dao.ensureTestIsRecorded(F.test(name = "mytest1"))
    dao.ensureTestIsRecorded(F.test(name = "nope"))

    dao.getTestNames("*test*") should contain theSameElementsAs (Seq("testsomething", "sometest", "mytest1"))
  }
}