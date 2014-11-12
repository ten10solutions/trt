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

abstract class DaoTest extends FlatSpec with Matchers with ExecutionDaoTest with GetTestsDaoTest with CiDaoTest {

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

    val Seq(test) = dao.getTestsById(Seq(testId))
    test.name should equal(DummyData.TestName)
    test.groupOpt should equal(Some(DummyData.Group))
  }

  "Recording an already existing test" should "have no effect" in transaction { dao ⇒
    val test = F.test(name = DummyData.TestName, groupOpt = Some(DummyData.Group))

    val testId = dao.ensureTestIsRecorded(test)
    val testIdAgain = dao.ensureTestIsRecorded(test)

    testIdAgain should equal(testId)
    dao.getTestIds() should equal(Seq(testId))
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

    val Some(enrichedBatch) = dao.getBatch(batchId)
    val batch = enrichedBatch.batch
    val Some(log) = enrichedBatch.logOpt
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

    val Some(enrichedBatch) = dao.getBatch(batchId)
    val batch = enrichedBatch.batch
    batch.urlOpt should equal(None)
    batch.durationOpt should equal(None)
    batch.nameOpt should equal(None)
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

  "Getting batches" should "return them all most recent batch first" in transaction { dao ⇒
    val batchId1 = dao.newBatch(F.batch(executionTime = 1.day.ago))
    val batchId3 = dao.newBatch(F.batch(executionTime = 3.days.ago))
    val batchId2 = dao.newBatch(F.batch(executionTime = 2.days.ago))

    val batches = dao.getBatches()

    batches.map(_.id) should equal(List(batchId1, batchId2, batchId3))
  }

  "Getting batches" should "let you filter by CI job" in transaction { dao ⇒
    def addBatchAssociatedWithJob(jobId: Id[CiJob], buildUrl: URI) = {
      val batchId = dao.newBatch(F.batch())
      dao.newCiBuild(F.ciBuild(batchId, jobId, buildUrl = buildUrl))
      batchId
    }
    val jobId1 = dao.ensureCiJob(F.ciJob(url = DummyData.JobUrl))
    val jobId2 = dao.ensureCiJob(F.ciJob(url = DummyData.JobUrl2))
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

  "Getting batches" should "let you filter by pass/fail" in transaction { dao ⇒
    val batchId1 = dao.newBatch(F.batch(passed = true))
    val batchId2 = dao.newBatch(F.batch(passed = false))

    dao.getBatches(resultOpt = Some(true)).map(_.id) should equal(Seq(batchId1))
    dao.getBatches(resultOpt = Some(false)).map(_.id) should equal(Seq(batchId2))
    dao.getBatches(resultOpt = None).map(_.id) should contain theSameElementsAs (Seq(batchId1, batchId2))
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
      status = TestStatus.Healthy,
      weather = DummyData.Weather,
      consecutiveFailures = DummyData.ConsecutiveFailures,
      failingSinceOpt = Some(failedExecutionTime),
      lastPassedExecutionIdOpt = Some(passedExecutionId),
      lastPassedTimeOpt = Some(passedExecutionTime),
      lastFailedExecutionIdOpt = Some(failedExecutionId),
      lastFailedTimeOpt = Some(failedExecutionTime),
      whenAnalysed = DummyData.WhenAnalysed,
      medianDurationOpt = Some(DummyData.Duration)))

    val analysis = dao.getEnrichedTest(testId).get.analysisOpt.get
    analysis.testId should equal(testId)
    analysis.status should equal(TestStatus.Healthy)
    analysis.weather should equal(DummyData.Weather)
    analysis.consecutiveFailures should equal(DummyData.ConsecutiveFailures)
    analysis.lastExecutionTime should equal(passedExecutionTime)
    analysis.lastExecutionId should equal(passedExecutionId)
    analysis.lastPassedTimeOpt should equal(Some(passedExecutionTime))
    analysis.lastPassedExecutionIdOpt should equal(Some(passedExecutionId))
    analysis.lastFailedTimeOpt should equal(Some(failedExecutionTime))
    analysis.lastFailedExecutionIdOpt should equal(Some(failedExecutionId))
    analysis.whenAnalysed should equal(DummyData.WhenAnalysed)
    analysis.medianDurationOpt should equal(Some(DummyData.Duration))

    dao.upsertAnalysis(F.analysis(
      testId = testId,
      status = TestStatus.Warning,
      weather = DummyData.Weather,
      consecutiveFailures = DummyData.ConsecutiveFailures,
      failingSinceOpt = None,
      lastPassedExecutionIdOpt = None,
      lastPassedTimeOpt = None,
      lastFailedExecutionIdOpt = None,
      lastFailedTimeOpt = None,
      whenAnalysed = DummyData.WhenAnalysed,
      medianDurationOpt = None))

    val analysis2 = dao.getEnrichedTest(testId).get.analysisOpt.get
    analysis2.testId should equal(testId)
    analysis2.status should equal(TestStatus.Warning)
    analysis2.weather should equal(DummyData.Weather)
    analysis2.consecutiveFailures should equal(DummyData.ConsecutiveFailures)
    analysis2.lastPassedTimeOpt should equal(None)
    analysis2.lastPassedExecutionIdOpt should equal(None)
    analysis2.lastFailedTimeOpt should equal(None)
    analysis2.lastFailedExecutionIdOpt should equal(None)
    analysis2.whenAnalysed should equal(DummyData.WhenAnalysed)
    analysis2.medianDurationOpt should equal(None)

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

    val analysis = dao.getEnrichedTest(testId).get.analysisOpt.get
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
    val jobId = dao.ensureCiJob(F.ciJob())
    dao.newCiBuild(F.ciBuild(batchId, jobId = jobId, buildUrl = DummyData.BuildUrl))
    dao.upsertAnalysis(F.analysis(testId, lastPassedExecutionIdOpt = Some(executionId)))

    dao.deleteBatches(List(batchId))

    dao.getBatch(batchId) should equal(None)
    dao.getEnrichedExecutionsInBatch(batchId) should equal(List())
    dao.getCiBuild(DummyData.BuildUrl) should equal(None)
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

    val DeleteBatchResult(Seq(affectedTestId), _) = dao.deleteBatches(List(batchId1))

    affectedTestId should be(testId1) // It hasn't been deleted, but it has had some executions removed
    dao.getEnrichedTest(testId2) should be(None) // It has had all its executions removed, so it has been removed too
  }

  "Deleting a batch" should "delete data associated with a test" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.addCategories(Seq(F.testCategory(testId)))
    dao.setTestComment(testId, DummyData.Comment)
    val batchId = dao.newBatch(F.batch())
    val executionId = dao.newExecution(F.execution(batchId, testId))
    dao.setExecutionComment(executionId, DummyData.Comment)
    dao.setBatchComment(batchId, DummyData.Comment)

    dao.deleteBatches(List(batchId))

    dao.getCategoryNames("*") should equal(Seq())
  }

  "Deleting a batch" should "return the ids of the deleted execution IDs" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch())
    val testId = dao.ensureTestIsRecorded(F.test())
    val executionId1 = dao.newExecution(F.execution(batchId, testId))
    val executionId2 = dao.newExecution(F.execution(batchId, testId))
    val executionId3 = dao.newExecution(F.execution(batchId, testId))

    val DeleteBatchResult(_, deletedExecutionIds) = dao.deleteBatches(List(batchId))

    deletedExecutionIds should contain theSameElementsAs (Seq(executionId1, executionId2, executionId3))
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
      projectNameOpt = Some(DummyData.ProjectName),
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

  it should "do case-insensitive matching" in transaction { dao ⇒
    dao.ensureTestIsRecorded(F.test(name = "a"))
    dao.ensureTestIsRecorded(F.test(name = "A"))

    dao.getTestNames("a") should contain theSameElementsAs (Seq("a", "A"))
  }

  it should "find matches with wildcards" in transaction { dao ⇒
    dao.ensureTestIsRecorded(F.test(name = "testsomething"))
    dao.ensureTestIsRecorded(F.test(name = "sometest"))
    dao.ensureTestIsRecorded(F.test(name = "mytest1"))
    dao.ensureTestIsRecorded(F.test(name = "nope"))

    dao.getTestNames("*test*") should contain theSameElementsAs (Seq("testsomething", "sometest", "mytest1"))
  }

  "Getting group names" should "find matches with wildcards" in transaction { dao ⇒
    dao.ensureTestIsRecorded(F.test(groupOpt = Some("myTEST")))
    dao.ensureTestIsRecorded(F.test(groupOpt = Some("test")))

    dao.getGroups("*test*") should contain theSameElementsAs (Seq("myTEST", "test"))
  }

  "Getting category names" should "find matches" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.addCategories(Seq(F.testCategory(testId, "category")))

    dao.getCategoryNames("*ateg*") should equal(Seq("category"))
  }

  "Caching" should "not prevent updates from being visible" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val batchId = dao.newBatch(F.batch())

    dao.getConfigurations() should equal(Seq())
    dao.countExecutions() should equal(0)

    val executionId = dao.newExecution(F.execution(batchId, testId, configuration = DummyData.Configuration1))

    dao.getConfigurations() should equal(Seq(DummyData.Configuration1))
    dao.countExecutions() should equal(1)

    dao.deleteBatches(List(batchId))

    dao.getConfigurations() should equal(Seq())
    dao.countExecutions() should equal(0)
  }

  "Tests" should "be able to have comments attached" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId, configuration = Configuration.Default))
    def getComment() = dao.getEnrichedTest(testId, Configuration.Default).get.commentOpt

    getComment() should equal(None)

    dao.setTestComment(testId, DummyData.Comment)

    getComment() should equal(Some(DummyData.Comment))

    dao.deleteTestComment(testId)

    getComment() should equal(None)
  }

  "Batches" should "be able to have comments attached" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch())
    def getComment() = dao.getBatch(batchId).get.commentOpt

    getComment() should equal(None)

    dao.setBatchComment(batchId, DummyData.Comment)

    getComment() should equal(Some(DummyData.Comment))

    dao.deleteBatchComment(batchId)

    getComment() should equal(None)
  }

  "Getting deleted tests" should "work" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.markTestsAsDeleted(Seq(testId))
    dao.getDeletedTests().map(_.id) should equal(Seq(testId))
    dao.markTestsAsDeleted(Seq(testId), deleted = false)
    dao.getDeletedTests().map(_.id) should equal(Seq())
  }

  "Setting batch duration" should "work" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch(durationOpt = None))
    dao.setBatchDuration(batchId, Some(DummyData.Duration))
    val Some(batch) = dao.getBatch(batchId)
    batch.batch.durationOpt should equal(Some(DummyData.Duration))
  }

  "Updating a batch" should "work" in transaction { dao ⇒
    val batchId = dao.newBatch(F.batch(
      durationOpt = None,
      passed = false,
      passCount = 0,
      failCount = 0,
      totalCount = 0))
    val batch = dao.getBatch(batchId).get.batch

    val updatedBatch = batch.copy(
      durationOpt = Some(DummyData.Duration),
      passed = true,
      passCount = 1,
      failCount = 2,
      totalCount = 3)
    dao.updateBatch(updatedBatch)

    val batchAgain = dao.getBatch(batchId).get.batch
    batchAgain should equal(updatedBatch)
  }

  "Adding and retrieving test categories" should "work" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    val category1 = F.testCategory(testId, "Category1", isUserCategory = true)
    dao.addCategories(Seq(category1))

    dao.getCategories(testId) should equal(Seq(category1))

    val category2 = F.testCategory(testId, "Category2")
    dao.addCategories(Seq(category2))

    dao.getCategories(testId) should contain theSameElementsAs Seq(category1, category2)
  }

  "Removing categories" should "work" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.addCategories(Seq(F.testCategory(testId)))

    dao.removeCategories(testId, Seq(DummyData.Category))

    dao.getCategories(testId) should equal(Seq())
  }

}