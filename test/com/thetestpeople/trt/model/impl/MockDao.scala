package com.thetestpeople.trt.model.impl

import java.net.URI
import org.joda.time.DateTime
import org.joda.time.Interval
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.github.nscala_time.time.Imports._
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.Lock
import org.apache.oro.text.GlobCompiler
import java.util.regex.Pattern

class MockDao extends Dao {

  private val lock: Lock = new ReentrantLock

  def transaction[T](p: ⇒ T): T = {
    lock.lock()
    try
      p
    finally
      lock.unlock()
  }

  private var executions: Seq[Execution] = List()
  private var tests: Seq[Test] = List()
  private var testComments: Seq[TestComment] = List()
  private var testCategories: Seq[TestCategory] = List()
  private var batches: Seq[Batch] = List()
  private var analyses: Seq[Analysis] = List()
  private var executionLogs: Seq[ExecutionLogRow] = List()
  private var executionComments: Seq[ExecutionComment] = List()
  private var batchLogs: Seq[BatchLogRow] = List()
  private var batchComments: Seq[BatchComment] = List()
  private var ciJobs: Seq[CiJob] = List()
  private var ciBuilds: Seq[CiBuild] = List()
  private var importSpecs: Seq[CiImportSpec] = List()
  private var systemConfiguration = SystemConfiguration()
  private var jenkinsConfiguration = JenkinsConfiguration()
  private var jenkinsConfigParams: Seq[JenkinsJobParam] = List()
  private var teamCityConfiguration = TeamCityConfiguration()

  def getEnrichedExecution(id: Id[Execution]): Option[EnrichedExecution] =
    for {
      execution ← executions.find(_.id == id)
      test ← tests.find(_.id == execution.testId)
      batch ← batches.find(_.id == execution.batchId)
      logOpt = executionLogs.find(_.executionId == id).map(_.log)
      commentOpt = executionComments.find(_.executionId == id).map(_.text)
    } yield EnrichedExecution(execution, test.qualifiedName, batch.nameOpt, logOpt, commentOpt)

  def getEnrichedTest(id: Id[Test], configuration: Configuration): Option[EnrichedTest] =
    for {
      test ← tests.find(_.id == id)
      analysis ← analyses.find(a ⇒ a.testId == test.id && a.configuration == configuration)
      commentOpt = testComments.find(_.testId == id).map(_.text)
    } yield EnrichedTest(test, Some(analysis), commentOpt)

  def getTestIds(): Seq[Id[Test]] = tests.map(_.id)

  /**
   * @return all tests marked as deleted
   */
  def getDeletedTests(): Seq[Test] =
    for {
      test ← tests
      if test.deleted
    } yield test

  def getAnalysedTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int],
    sortBy: SortBy.Test = SortBy.Test.Group()): Seq[EnrichedTest] = {
    val allResults =
      for {
        test ← tests
        if groupOpt.forall(pattern ⇒ test.groupOpt.exists(group ⇒ matchesPattern(pattern, group)))
        if nameOpt.forall(pattern ⇒ matchesPattern(pattern, test.name))
        analysisOpt = analyses.find(a ⇒ a.testId == test.id && a.configuration == configuration)
        if testStatusOpt.forall(status ⇒ analysisOpt.exists(_.status == status))
        commentOpt = testComments.find(_.testId == test.id).map(_.text)
      } yield EnrichedTest(test, analysisOpt, commentOpt)
    def order(x: Seq[EnrichedTest], descending: Boolean) = if (descending) x.reverse else x
    val sortedResults = sortBy match {
      case SortBy.Test.Weather(descending) ⇒
        order(allResults.sortBy(_.analysisOpt.map(_.weather)), descending)
      case SortBy.Test.Group(descending) ⇒
        order(allResults.sortBy(_.test.name).sortBy(_.test.groupOpt), descending)
      case SortBy.Test.Name(descending) ⇒
        order(allResults.sortBy(_.name), descending)
      case SortBy.Test.Duration(descending) ⇒
        order(allResults.sortBy(_.analysisOpt.flatMap(_.medianDurationOpt)), descending)
      case SortBy.Test.ConsecutiveFailures(descending) ⇒
        order(allResults.sortBy(_.analysisOpt.map(_.consecutiveFailures)), descending)
      case SortBy.Test.StartedFailing(descending) ⇒
        order(allResults.sortBy(_.analysisOpt.flatMap(_.failingSinceOpt)), descending)
      case SortBy.Test.LastPassed(descending) ⇒
        order(allResults.sortBy(_.analysisOpt.flatMap(_.lastPassedTimeOpt)), descending)
      case SortBy.Test.LastFailed(descending) ⇒
        order(allResults.sortBy(_.analysisOpt.flatMap(_.lastFailedTimeOpt)), descending)
    }
    limitOpt match {
      case Some(limit) ⇒ sortedResults.drop(startingFrom).take(limit)
      case None        ⇒ sortedResults.drop(startingFrom)
    }
  }

  def getTestsById(testIds: Seq[Id[Test]]): Seq[Test] = tests.filter(test ⇒ testIds.contains(test.id))

  def getTestCountsByConfiguration(): Map[Configuration, TestCounts] =
    getConfigurations().map { c ⇒ c -> getTestCounts(c) }.toMap

  def getTestCounts(configuration: Configuration, nameOpt: Option[String] = None, groupOpt: Option[String] = None, categoryOpt: Option[String] = None): TestCounts = {
    val tests = getAnalysedTests(configuration, nameOpt = nameOpt, groupOpt = groupOpt, categoryOpt = categoryOpt)
    val passed = tests.count(_.analysisOpt.exists(_.status == TestStatus.Healthy))
    val warning = tests.count(_.analysisOpt.exists(_.status == TestStatus.Warning))
    val failed = tests.count(_.analysisOpt.exists(_.status == TestStatus.Broken))
    TestCounts(passed, warning, failed)
  }

  def upsertAnalysis(analysis: Analysis) {
    analyses = analysis +: analyses.filterNot(_.testId == analysis.testId)
  }

  def getBatch(id: Id[Batch]): Option[BatchAndLog] = batches.find(_.id == id).map { batch ⇒
    val logOpt = batchLogs.find(_.batchId == id).map(_.log)
    val importSpecIdOpt = ciBuilds.find(_.batchId == id).flatMap(_.importSpecIdOpt)
    val commentOpt = batchComments.find(_.batchId == id).map(_.text)
    BatchAndLog(batch, logOpt, importSpecIdOpt, commentOpt)
  }

  def getBatches(jobIdOpt: Option[Id[CiJob]] = None, configurationOpt: Option[Configuration] = None, resultOpt: Option[Boolean]): Seq[Batch] = {
    batches
      .filter(batch ⇒ jobIdOpt.forall(jobId ⇒ areAssociated(batch, jobId)))
      .filter(batch ⇒ configurationOpt.forall(configuration ⇒ batch.configurationOpt == Some(configuration)))
      .filter(batch ⇒ resultOpt.forall(result ⇒ batch.passed == result))
      .sortBy(_.executionTime)
      .reverse
  }

  private def areAssociated(batch: Batch, jobId: Id[CiJob]): Boolean = {
    for {
      build ← ciBuilds
      job ← ciJobs
      if job.id == jobId
      if build.jobId == job.id
      if build.batchId == batch.id
    } return true
    false
  }

  def getEnrichedExecutionsInBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean]): Seq[EnrichedExecution] =
    for {
      batch ← batches
      if batch.id == batchId
      execution ← executions
      if execution.batchId == batch.id
      test ← tests.find(_.id == execution.testId)
      if passedFilterOpt.forall(expected ⇒ execution.passed == expected)
    } yield EnrichedExecution(execution, test.qualifiedName, batch.nameOpt, logOpt = None, commentOpt = None)

  def getEnrichedExecutions(ids: Seq[Id[Execution]]): Seq[EnrichedExecution] =
    for {
      batch ← batches
      execution ← executions.filter(_.batchId == batch.id)
      if execution.batchId == batch.id
      if ids.contains(execution.id)
      test ← tests.find(_.id == execution.testId)
    } yield EnrichedExecution(execution, test.qualifiedName, batch.nameOpt, logOpt = None, commentOpt = None)

  def getExecutionsForTest(id: Id[Test]): Seq[Execution] =
    executions.filter(_.testId == id).sortBy(_.executionTime).reverse

  def getEnrichedExecutionsForTest(testId: Id[Test], configurationOpt: Option[Configuration], resultOpt: Option[Boolean] = None): Seq[EnrichedExecution] = {
    val executionsForTest =
      for {
        test ← tests.filter(_.id == testId)
        execution ← executions.filter(_.testId == testId)
        batch ← batches.find(_.id == execution.batchId)
        if configurationOpt.forall(_ == execution.configuration)
        if resultOpt.forall(_ == execution.passed)
      } yield EnrichedExecution(execution, test.qualifiedName, batch.nameOpt, logOpt = None, commentOpt = None)
    executionsForTest.sortBy(_.execution.executionTime).reverse
  }

  private def isDeleted(testId: Id[Test]) = tests.find(_.id == testId).exists(_.deleted)

  def iterateAllExecutions[T](f: Iterator[ExecutionLite] ⇒ T): T =
    f(executions.filterNot(e ⇒ isDeleted(e.testId)).sortBy(e ⇒ (e.configuration, e.testId, e.executionTime)).map(executionLite).iterator)

  private def executionLite(execution: Execution) =
    ExecutionLite(
      testId = execution.testId,
      executionTime = execution.executionTime,
      passed = execution.passed,
      configuration = execution.configuration)

  def getExecutionIntervalsByConfig(): Map[Configuration, Interval] =
    executions.groupBy(_.configuration).map {
      case (configuration, configExecutions) ⇒
        val executionTimes = configExecutions.map(_.executionTime)
        configuration -> new Interval(executionTimes.min, executionTimes.max)
    }

  def getEnrichedExecutions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean] = None, startingFrom: Int, limit: Int): Seq[EnrichedExecution] = {
    val all =
      for {
        batch ← batches
        execution ← executions.filter(_.batchId == batch.id)
        if configurationOpt.forall(c ⇒ c == execution.configuration)
        if resultOpt.forall(c ⇒ c == execution.passed)
        test ← tests.find(_.id == execution.testId)
      } yield EnrichedExecution(execution, test.qualifiedName, batch.nameOpt, logOpt = None, commentOpt = None)
    all.sortBy(_.qualifiedName.name).sortBy(_.qualifiedName.groupOpt).reverse.sortBy(_.executionTime).reverse.drop(startingFrom).take(limit)
  }

  def countExecutions(configurationOpt: Option[Configuration], resultOpt: Option[Boolean] = None): Int =
    executions.count(e ⇒ configurationOpt.forall(_ == e.configuration) && resultOpt.forall(_ == e.passed))

  private def nextId[T <: EntityType](ids: Seq[Id[T]]): Id[T] = {
    val allIds = ids.map(_.value)
    Id(if (allIds.isEmpty) 1 else allIds.max + 1)
  }

  def newBatch(batch: Batch, logOpt: Option[String]): Id[Batch] = {
    val newId = nextId(batches.map(_.id))
    batches +:= batch.copy(id = newId)
    for (log ← logOpt)
      batchLogs +:= BatchLogRow(newId, log)
    newId
  }

  def deleteBatches(batchIds: Seq[Id[Batch]]) = {
    val (executionIds, testIds) = executions.filter(batchIds contains _.batchId).map(e ⇒ (e.id, e.testId)).toList.unzip
    ciBuilds = ciBuilds.filterNot(batchIds contains _.batchId)
    analyses = analyses.filterNot(testIds contains _.testId)
    executionLogs = executionLogs.filterNot(executionIds contains _.executionId)
    executions = executions.filterNot(executionIds contains _.id)
    executionComments = executionComments.filterNot(executionIds contains _.executionId)
    batchLogs = batchLogs.filterNot(batchIds contains _.batchId)
    batches = batches.filterNot(batchIds contains _.id)
    batchComments = batchComments.filterNot(batchIds contains _.batchId)
    val (deleteTestIds, affectedTestIds) = testIds.partition(getExecutionsForTest(_).isEmpty)
    tests = tests.filterNot(deleteTestIds contains _.id)
    testComments = testComments.filterNot(testIds contains _.testId)
    testCategories = testCategories.filterNot(testIds contains _.testId)
    DeleteBatchResult(affectedTestIds, executionIds)
  }

  private def newTest(test: Test): Id[Test] = {
    val newId = nextId(tests.map(_.id))
    tests +:= test.copy(id = newId)
    newId
  }

  def ensureTestIsRecorded(test: Test): Id[Test] = {
    tests.find(_.qualifiedName == test.qualifiedName) match {
      case Some(test) ⇒
        test.id
      case None ⇒
        newTest(test)
    }
  }

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true) {
    tests =
      tests.filterNot(ids contains _.id) ++
        tests.filter(ids contains _.id).map(_.copy(deleted = deleted))
  }

  def newExecution(execution: Execution, logOpt: Option[String]): Id[Execution] = {
    val newId = nextId(executions.map(_.id))
    executions +:= execution.copy(id = newId)
    for (log ← logOpt)
      executionLogs +:= ExecutionLogRow(newId, log)
    newId
  }

  def getExecutionLog(id: Id[Execution]) = executionLogs.find(_.executionId == id).map(_.log)

  def newCiBuild(ciBuild: CiBuild) {
    ciBuilds +:= ciBuild
  }

  def getCiBuild(buildUrl: URI): Option[CiBuild] =
    ciBuilds.find(_.buildUrl == buildUrl)

  def getCiBuildUrls(): Seq[URI] = ciBuilds.map(_.buildUrl)

  def getCiJobs(): Seq[CiJob] = ciJobs

  def getCiBuilds(specId: Id[CiImportSpec]): Seq[CiBuild] =
    for {
      build ← ciBuilds
      if build.importSpecIdOpt == Some(specId)
    } yield build

  def newCiImportSpec(spec: CiImportSpec): Id[CiImportSpec] = {
    val newId = nextId(importSpecs.map(_.id))
    importSpecs +:= spec.copy(id = newId)
    newId
  }

  def getCiImportSpecs: Seq[CiImportSpec] = importSpecs

  def deleteCiImportSpec(id: Id[CiImportSpec]): Boolean = {
    val found = importSpecs.exists(_.id == id)
    importSpecs = importSpecs.filterNot(_.id == id)
    found
  }

  def getCiImportSpec(id: Id[CiImportSpec]): Option[CiImportSpec] = importSpecs.find(_.id == id)

  def updateCiImportSpec(updatedSpec: CiImportSpec): Boolean =
    importSpecs.find(_.id == updatedSpec.id) match {
      case Some(spec) ⇒
        importSpecs = updatedSpec +: importSpecs.filterNot(_.id == updatedSpec.id)
        true
      case None ⇒
        false
    }

  def updateCiImportSpec(id: Id[CiImportSpec], lastCheckedOpt: Option[DateTime]): Boolean =
    importSpecs.find(_.id == id) match {
      case Some(spec) ⇒
        val updatedSpec = spec.copy(lastCheckedOpt = lastCheckedOpt)
        importSpecs = updatedSpec +: importSpecs.filterNot(_.id == id)
        true
      case None ⇒
        false
    }

  def getSystemConfiguration(): SystemConfiguration = systemConfiguration

  def updateSystemConfiguration(newConfig: SystemConfiguration) { systemConfiguration = newConfig }

  def getJenkinsConfiguration(): FullJenkinsConfiguration = FullJenkinsConfiguration(jenkinsConfiguration, jenkinsConfigParams.toList)

  def updateJenkinsConfiguration(config: FullJenkinsConfiguration) {
    jenkinsConfiguration = config.config
    jenkinsConfigParams = config.params
  }

  def ensureCiJob(job: CiJob): Id[CiJob] = {
    ciJobs.find(_.url == job.url) match {
      case Some(jobAgain) ⇒
        jobAgain.id
      case None ⇒
        val newId = nextId(ciJobs.map(_.id))
        ciJobs +:= job.copy(id = newId)
        newId
    }
  }

  def getConfigurations(): Seq[Configuration] = executions.map(_.configuration).distinct.sorted

  def getConfigurations(testId: Id[Test]): Seq[Configuration] =
    executions.filter(_.testId == testId).map(_.configuration).distinct.sorted

  private def matchesPattern(pattern: String, text: String) =
    globToRegex(pattern).matcher(text).matches()

  private def globToRegex(pattern: String): Pattern =
    Pattern.compile(GlobCompiler.globToPerl5(pattern.toCharArray, GlobCompiler.CASE_INSENSITIVE_MASK), Pattern.CASE_INSENSITIVE)

  def getTestNames(pattern: String): Seq[String] = {
    val matches = globMatcher(pattern)
    for (test ← tests if matches(test.name))
      yield test.name
  }

  def getGroups(pattern: String): Seq[String] = {
    val matches = globMatcher(pattern)
    for (test ← tests; group ← test.groupOpt if matches(group))
      yield group
  }

  def getCategoryNames(pattern: String): Seq[String] = {
    val matches = globMatcher(pattern)
    for (category ← testCategories if matches(category.category))
      yield category.category
  }

  private def globMatcher(pattern: String): String ⇒ Boolean = {
    val regexPattern = globToRegex(pattern)
    def matches(s: String) = regexPattern.matcher(s).matches()
    matches
  }

  def setExecutionComment(id: Id[Execution], text: String) =
    executionComments = ExecutionComment(id, text) +: executionComments.filterNot(_.executionId == id)

  def deleteExecutionComment(id: Id[Execution]) = executionComments = executionComments.filterNot(_.executionId == id)

  def setBatchComment(id: Id[Batch], text: String) =
    batchComments = BatchComment(id, text) +: batchComments.filterNot(_.batchId == id)

  def deleteBatchComment(id: Id[Batch]) = batchComments = batchComments.filterNot(_.batchId == id)

  def setTestComment(id: Id[Test], text: String) =
    testComments = TestComment(id, text) +: testComments.filterNot(_.testId == id)

  def deleteTestComment(id: Id[Test]) = testComments = testComments.filterNot(_.testId == id)

  def getTeamCityConfiguration(): TeamCityConfiguration = teamCityConfiguration

  def updateTeamCityConfiguration(config: TeamCityConfiguration) = teamCityConfiguration = config

  def setBatchDuration(id: Id[Batch], durationOpt: Option[Duration]): Boolean = {
    val updatedBatches = batches.filter(_.id == id).map(_.copy(durationOpt = durationOpt))
    batches = batches.filterNot(_.id == id) ++ updatedBatches
    updatedBatches.nonEmpty
  }

  def getCategories(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[TestCategory]] =
    testCategories.filter(t ⇒ testIds contains t.testId).groupBy(_.testId)

  def addCategories(categories: Seq[TestCategory]) {
    testCategories ++:= categories
  }

  def removeCategories(testId: Id[Test], categories: Seq[String]) {
    testCategories = testCategories.filterNot(tc ⇒ tc.testId == testId && categories.contains(tc.category))
  }

}
