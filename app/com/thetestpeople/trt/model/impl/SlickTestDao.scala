package com.thetestpeople.trt.model.impl

import org.joda.time._
import com.github.tototoshi.slick.GenericJodaSupport
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.KeyedLocks
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.utils.LockUtils._
import javax.sql.DataSource
import java.net.URI
import scala.slick.util.CloseableIterator
import scala.slick.driver.H2Driver

trait SlickTestDao extends TestDao { this: SlickDao ⇒

  import driver.simple._
  import Database.dynamicSession
  import jodaSupport._
  import Mappers._
  import Tables._

  private lazy val testsAndAnalyses =
    for ((test, analysis) ← tests leftJoin analyses on (_.id === _.testId))
      yield (test, analysis)

  def getEnrichedTest(id: Id[Test], configuration: Configuration): Option[EnrichedTest] = {
    val query =
      for {
        ((test, analysis), comment) ← testsAndAnalyses leftJoin testComments on (_._1.id === _.testId)
        if test.id === id
        if analysis.configuration === configuration
      } yield (test, analysis.?, comment.?)
    query.firstOption.map { case (test, analysisOpt, commentOpt) ⇒ EnrichedTest(test, analysisOpt, commentOpt.map(_.text)) }
  }

  def getTestIds(): Seq[Id[Test]] =
    tests.map(_.id).run

  def getAnalysedTests(
    configuration: Configuration,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    blackListOpt: Option[Seq[Id[Test]]] = None,
    whiteListOpt: Option[Seq[Id[Test]]] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None,
    sortBy: SortBy.Test = SortBy.Test.Group()): Seq[EnrichedTest] = {
    var query: TestAnalysisQuery = testsAndAnalyses
    query = query.filter(_._2.configuration === configuration)
    query = query.filterNot(_._1.deleted)
    for (name ← nameOpt)
      query = query.filter(_._1.name.toLowerCase like globToSqlPattern(name))
    for (group ← groupOpt)
      query = query.filter(_._1.group.toLowerCase like globToSqlPattern(group))
    for (category ← categoryOpt)
      query = for {
        (test, analysis) ← query
        testCategory ← testCategories
        if test.id === testCategory.testId
        if testCategory.category === category
      } yield (test, analysis)
    for (status ← testStatusOpt)
      query = query.filter(_._2.status === status)
    for (whiteList ← whiteListOpt)
      query = query.filter(_._1.id inSet whiteList)
    for (blackList ← blackListOpt)
      query = query.filterNot(_._1.id inSet blackList)
    query = sortQuery(query, sortBy)
    query = query.drop(startingFrom)
    for (limit ← limitOpt)
      query = query.take(limit)
    query.map { case (test, analysis) ⇒ (test, analysis.?) }.run.map { case (test, analysisOpt) ⇒ EnrichedTest(test, analysisOpt, commentOpt = None) }
  }

  private type TestAnalysisQuery = Query[(TestMapping, AnalysisMapping), (Test, Analysis), Seq]

  private def sortQuery(query: TestAnalysisQuery, sortBy: SortBy.Test): TestAnalysisQuery =
    sortBy match {
      case SortBy.Test.Weather(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.weather, descending) }
      case SortBy.Test.Group(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(test.name, descending) }
          .sortBy { case (test, analysis) ⇒ order(test.group, descending) }
      case SortBy.Test.Name(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(test.name, descending) }
      case SortBy.Test.Duration(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.medianDuration, descending) }
      case SortBy.Test.ConsecutiveFailures(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.consecutiveFailures, descending) }
      case SortBy.Test.StartedFailing(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.failingSince, descending) }
      case SortBy.Test.LastPassed(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.lastPassedTime, descending) }
      case SortBy.Test.LastFailed(descending) ⇒
        query.sortBy { case (test, analysis) ⇒ order(analysis.lastFailedTime, descending) }
    }

  private def order[T](column: Column[T], descending: Boolean) =
    if (descending) column.desc else column.asc

  def getTestsById(testIds: Seq[Id[Test]]): Seq[Test] = tests.filter(_.id inSet testIds).run

  def getDeletedTests(): Seq[Test] = tests.filter(_.deleted).sortBy(_.name).sortBy(_.group).run

  def getTestCounts(
    configuration: Configuration,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    ignoredTests: Seq[Id[Test]] = Seq()): TestCounts = {

    var query = testsAndAnalyses
    query = query.filter(_._2.configuration === configuration)
    query = query.filterNot(_._1.deleted)
    query = query.filterNot(_._1.id inSet ignoredTests)
    for (name ← nameOpt)
      query = query.filter(_._1.name.toLowerCase like globToSqlPattern(name))
    for (group ← groupOpt)
      query = query.filter(_._1.group.toLowerCase like globToSqlPattern(group))
    for (category ← categoryOpt)
      query = for {
        (test, analysis) ← query
        testCategory ← testCategories
        if test.id === testCategory.testId
        if testCategory.category === category
      } yield (test, analysis)
    // Workaround for Slick exception if no analysis: "scala.slick.SlickException: Read NULL value for ResultSet column":
    query = query.filter(_._2.testId.?.isDefined)
    val results: Map[TestStatus, Int] =
      query.groupBy(_._2.status).map { case (status, results) ⇒ status -> results.length }.run.toMap
    def count(status: TestStatus) = results.collect { case (`status`, count) ⇒ count }.headOption.getOrElse(0)

    TestCounts(
      passed = count(TestStatus.Healthy),
      warning = count(TestStatus.Warning),
      failed = count(TestStatus.Broken),
      ignored = ignoredTests.size)
  }

  private def getTestWithName(name: Column[String]) =
    for {
      (test, analysis) ← testsAndAnalyses
      if test.name === name
    } yield (test, analysis.?)

  private lazy val getTestWithGroupCompiled = {
    def getTestWithNameAndGroup(name: Column[String], group: Column[String]) =
      getTestWithName(name).filter(_._1.group === group)
    Compiled(getTestWithNameAndGroup _)
  }

  private lazy val getTestWithoutGroupCompiled = {
    def getTestWithNameAndNoGroup(name: Column[String]) =
      getTestWithName(name).filter(_._1.group.isEmpty)
    Compiled(getTestWithNameAndNoGroup _)
  }

  private def getEnrichedTest(qualifiedName: QualifiedName): Option[EnrichedTest] = {
    val QualifiedName(name, groupOpt) = qualifiedName
    val query = groupOpt match {
      case Some(group) ⇒ getTestWithGroupCompiled(name, group)
      case None        ⇒ getTestWithoutGroupCompiled(name)
    }
    query.firstOption.map { case (test, analysisOpt) ⇒ EnrichedTest(test, analysisOpt, commentOpt = None) }
  }

  private lazy val testInserter = (tests returning tests.map(_.id)).insertInvoker

  def ensureTestIsRecorded(test: Test): Id[Test] = synchronized {
    getEnrichedTest(test.qualifiedName) match {
      case Some(testAndAnalysis) ⇒
        testAndAnalysis.id
      case None ⇒
        testInserter.insert(test)
    }
  }

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true): Unit =
    tests.filter(_.id.inSet(ids)).map(_.deleted).update(deleted)

  def getTestNames(pattern: String): Seq[String] =
    tests
      .filter(_.name.toLowerCase like globToSqlPattern(pattern))
      .groupBy(_.name).map(_._1).run

  def getGroups(pattern: String): Seq[String] =
    tests
      .filter(_.group.isDefined)
      .filter(_.group.toLowerCase like globToSqlPattern(pattern))
      .groupBy(_.group).map(_._1).run.flatten

  def getCategoryNames(pattern: String): Seq[String] =
    testCategories
      .filter(_.category.toLowerCase like globToSqlPattern(pattern))
      .groupBy(_.category).map(_._1).run

  private def globToSqlPattern(pattern: String) = pattern.replace("*", "%").toLowerCase

  private lazy val analysisInserter = analyses.insertInvoker

  private lazy val updateAnalysisCompiled = {
    def updateAnalysis(testId: Column[Id[Test]], configuration: Column[Configuration]) =
      for {
        analysis ← analyses
        if analysis.testId === testId
        if analysis.configuration === configuration
      } yield analysis
    Compiled(updateAnalysis _)
  }

  private lazy val getAnalysisCompiled = {
    def getAnalysis(testId: Column[Id[Test]], configuration: Column[Configuration]) =
      analyses.filter(_.testId === testId).filter(_.configuration === configuration)
    Compiled(getAnalysis _)
  }

  def upsertAnalysis(newAnalysis: Analysis): Unit = synchronized {
    getAnalysisCompiled(newAnalysis.testId, newAnalysis.configuration).firstOption match {
      case Some(analysis) ⇒
        updateAnalysisCompiled(newAnalysis.testId, newAnalysis.configuration).update(newAnalysis)
      case None ⇒
        analysisInserter.insert(newAnalysis)
    }
  }

  def setTestComment(id: Id[Test], text: String): Unit =
    if (testComments.filter(_.testId === id).firstOption.isDefined)
      testComments.filter(_.testId === id).map(_.text).update(text)
    else
      testComments.insert(TestComment(id, text))

  def deleteTestComment(id: Id[Test]): Unit = testComments.filter(_.testId === id).delete

  def getCategories(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[TestCategory]] =
    testCategories.filter(_.testId inSet testIds).run.groupBy(_.testId)

  private lazy val deleteTestCategoriesCompiled = {
    def deleteTestCategories(testId: Column[Id[Test]]) =
      testCategories.filter(_.testId === testId)
    Compiled(deleteTestCategories _)
  }

  private lazy val testCategoriesInserter = testCategories.insertInvoker

  def addCategories(categories: Seq[TestCategory]) {
    testCategoriesInserter.insertAll(categories: _*)
  }

  def removeCategories(testId: Id[Test], categories: Seq[String]) {
    testCategories.filter(tc ⇒ tc.testId === testId && tc.category.inSet(categories)).delete
  }

  def getConfigurations(testId: Id[Test]): Seq[Configuration] =
    executions.filter(_.testId === testId).groupBy(_.configuration).map(_._1).sorted.run

  def getIgnoredConfigurations(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[Configuration]] = {
    val query =
      for {
        (test, ignoredConfig) ← tests leftJoin ignoredTestConfigurations on (_.id === _.testId)
        if test.id inSet testIds
      } yield (test.id, ignoredConfig.?)
    val ignoredConfigs =
      for {
        (testId, ignoredConfigOpt) ← query.run
        ignoredConfig ← ignoredConfigOpt
      } yield ignoredConfig
    //    ignoredConfigs.groupBy(_.testId).map {
    //      case (testId, ignoredConfigs) ⇒ testId -> ignoredConfigs.map(_.configuration)
    //    }
    query.run.groupBy(_._1).map {
      case (testId, ignoredConfigs) ⇒
        testId -> ignoredConfigs.map(_._2).flatten.map(_.configuration)
    }

  }

  def getIgnoredTests(configuration: Configuration, nameOpt: Option[String] = None, groupOpt: Option[String] = None,
                      categoryOpt: Option[String] = None): Seq[Id[Test]] = {
    var query =
      for {
        ignoredConfig ← ignoredTestConfigurations
        test ← tests if test.id === ignoredConfig.testId
        if !test.deleted
        if ignoredConfig.configuration === configuration
      } yield test
    for (name ← nameOpt)
      query = query.filter(_.name.toLowerCase like globToSqlPattern(name))
    for (group ← groupOpt)
      query = query.filter(_.group.toLowerCase like globToSqlPattern(group))
    for (category ← categoryOpt)
      query = for {
        test ← query
        testCategory ← testCategories
        if test.id === testCategory.testId
        if testCategory.category === category
      } yield test
    query.map(_.id).run
  }

  def isTestIgnoredInConfiguration(testId: Id[Test], configuration: Configuration): Boolean =
    ignoredTestConfigurations.filter(c ⇒ c.testId === testId && c.configuration === configuration).exists.run

  def addIgnoredTestConfigurations(ignoredConfigs: Seq[IgnoredTestConfiguration]) {
    ignoredTestConfigurations.insertAll(ignoredConfigs: _*)
  }

  def removeIgnoredTestConfigurations(testIds: Seq[Id[Test]], configuration: Configuration) =
    ignoredTestConfigurations
      .filter(c ⇒ c.testId.inSet(testIds) && c.configuration === configuration)
      .delete

}