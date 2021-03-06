package com.thetestpeople.trt.model.impl

import org.joda.time.DateTime

import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.mother.{ TestDataFactory ⇒ F }

trait GetTestsDaoTest { self: DaoTest ⇒

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

    tests.map(_.id) should contain theSameElementsAs (Seq(testId1, testId2, testId3))
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

  "Getting tests" should "allow you to filter by group with wildcards" in transaction { dao ⇒
    val dao = createDao
    def addTest(group: String) = {
      val testId = dao.ensureTestIsRecorded(F.test(groupOpt = Some(group)))
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val testId1 = addTest("Foobar")
    val testId2 = addTest("Foobaz")
    val testId3 = addTest("Quux")

    val tests = dao.getAnalysedTests(groupOpt = Some("Foo*")).map(_.test)

    tests.map(_.id) should contain theSameElementsAs (Seq(testId1, testId2))
  }

  "Getting tests" should "allow you to filter by test name" in transaction { dao ⇒
    val dao = createDao
    def addTest(name: String) = {
      val testId = dao.ensureTestIsRecorded(F.test(name = name))
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val testId1 = addTest("some test 1")
    val testId2 = addTest("some test 2")
    val testId3 = addTest("Another thing")

    val testIds = dao.getAnalysedTests(nameOpt = Some("*test*")).map(_.id)
    testIds should contain theSameElementsAs (Seq(testId1, testId2))
  }

  "Getting tests" should "allow you to filter by status" in transaction { dao ⇒
    val dao = createDao
    def addTest(status: TestStatus) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, status))
      testId
    }
    val testId1 = addTest(TestStatus.Healthy)
    val testId2 = addTest(TestStatus.Warning)
    val testId3 = addTest(TestStatus.Warning)
    val testId4 = addTest(TestStatus.Broken)
    val testId5 = addTest(TestStatus.Broken)
    val testId6 = addTest(TestStatus.Broken)

    def getTestIds(status: TestStatus) = dao.getAnalysedTests(testStatusOpt = Some(status)).map(_.id)
    getTestIds(TestStatus.Healthy) should contain theSameElementsAs (List(testId1))
    getTestIds(TestStatus.Warning) should contain theSameElementsAs (List(testId2, testId3))
    getTestIds(TestStatus.Broken) should contain theSameElementsAs (List(testId4, testId5, testId6))
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

    dao.getAnalysedTests(startingFrom = 3, limitOpt = Some(2)).map(_.id) should contain theSameElementsAs (List(testId3, testId4))
    dao.getAnalysedTests(startingFrom = 4, limitOpt = None).map(_.id) should contain theSameElementsAs (List(testId4, testId5))
  }

  "Getting tests" should "by default return results ordered by group, then name" in transaction { dao ⇒
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

    dao.getAnalysedTests().map(_.id) should equal(Seq(
      testId_A0, testId_A1, testId_B0, testId_B1, testId_C0, testId_C1))
  }

  "Getting tests" should "support ordering by weather" in transaction { dao ⇒
    def addTest(weather: Double) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, weather = weather))
      testId
    }
    val test1 = addTest(weather = 0.0)
    val test3 = addTest(weather = 1.0)
    val test2 = addTest(weather = 0.5)

    dao.getAnalysedTests(sortBy = SortBy.Test.Weather()).map(_.id) should equal(Seq(test1, test2, test3))
    dao.getAnalysedTests(sortBy = SortBy.Test.Weather(descending = true)).map(_.id) should equal(Seq(test3, test2, test1))
  }

  "Getting tests" should "support ordering by name" in transaction { dao ⇒
    def addTest(name: String, group: String) = {
      val testId = dao.ensureTestIsRecorded(F.test(name, Some(group)))
      dao.upsertAnalysis(F.analysis(testId))
      testId
    }
    val test1 = addTest(name = "Aardvark", group = "Zebra")
    val test3 = addTest(name = "Cat", group = "Xenomorph")
    val test2 = addTest(name = "Badger", group = "Yeti")

    dao.getAnalysedTests(sortBy = SortBy.Test.Name()).map(_.id) should equal(Seq(test1, test2, test3))
    dao.getAnalysedTests(sortBy = SortBy.Test.Name(descending = true)).map(_.id) should equal(Seq(test3, test2, test1))
  }

  "Getting tests" should "support ordering by consecutive failures" in transaction { dao ⇒
    def addTest(consecutiveFailures: Int) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, consecutiveFailures = consecutiveFailures))
      testId
    }
    val test1 = addTest(consecutiveFailures = 1)
    val test3 = addTest(consecutiveFailures = 3)
    val test2 = addTest(consecutiveFailures = 2)

    dao.getAnalysedTests(sortBy = SortBy.Test.ConsecutiveFailures()).map(_.id) should equal(Seq(test1, test2, test3))
    dao.getAnalysedTests(sortBy = SortBy.Test.ConsecutiveFailures(descending = true)).map(_.id) should equal(Seq(test3, test2, test1))
  }

  "Getting tests" should "support ordering by started failing" in transaction { dao ⇒
    def addTest(failingSince: Option[DateTime]) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, failingSinceOpt = failingSince))
      testId
    }
    val test1 = addTest(failingSince = None)
    val test3 = addTest(failingSince = Some(2.days.ago))
    val test2 = addTest(failingSince = Some(3.days.ago))

    dao.getAnalysedTests(sortBy = SortBy.Test.StartedFailing()).map(_.id) should equal(Seq(test1, test2, test3))
    dao.getAnalysedTests(sortBy = SortBy.Test.StartedFailing(descending = true)).map(_.id) should equal(Seq(test3, test2, test1))
  }

  "Getting tests" should "support ordering by last passed" in transaction { dao ⇒
    def addTest(lastPassed: Option[DateTime]) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, lastPassedTimeOpt = lastPassed))
      testId
    }
    val test1 = addTest(lastPassed = None)
    val test3 = addTest(lastPassed = Some(2.days.ago))
    val test2 = addTest(lastPassed = Some(3.days.ago))

    dao.getAnalysedTests(sortBy = SortBy.Test.LastPassed()).map(_.id) should equal(Seq(test1, test2, test3))
    dao.getAnalysedTests(sortBy = SortBy.Test.LastPassed(descending = true)).map(_.id) should equal(Seq(test3, test2, test1))
  }

  "Getting tests" should "support ordering by last failed" in transaction { dao ⇒
    def addTest(lastFailed: Option[DateTime]) = {
      val testId = dao.ensureTestIsRecorded(F.test())
      dao.upsertAnalysis(F.analysis(testId, lastFailedTimeOpt = lastFailed))
      testId
    }
    val test1 = addTest(lastFailed = None)
    val test3 = addTest(lastFailed = Some(2.days.ago))
    val test2 = addTest(lastFailed = Some(3.days.ago))

    dao.getAnalysedTests(sortBy = SortBy.Test.LastFailed()).map(_.id) should equal(Seq(test1, test2, test3))
    dao.getAnalysedTests(sortBy = SortBy.Test.LastFailed(descending = true)).map(_.id) should equal(Seq(test3, test2, test1))
  }

  "Getting tests" should "respect blacklist" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId1))
    val testId2 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId2))

    dao.getAnalysedTests(blackListOpt = Some(Seq(testId1))).map(_.id) shouldEqual (Seq(testId2))
  }

  "Getting tests" should "respect whitelist" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId1))
    val testId2 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId2))

    dao.getAnalysedTests(whiteListOpt = Some(Seq(testId1))).map(_.id) shouldEqual (Seq(testId1))
  }

  "Getting tests" should "filter by category" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId1))
    dao.addCategories(Seq(F.testCategory(testId1, category = DummyData.Category)))

    val testId2 = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId2))

    dao.getAnalysedTests(categoryOpt = Some(DummyData.Category)).map(_.id) shouldEqual (Seq(testId1))
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
    addTest(TestStatus.Healthy)
    addTest(TestStatus.Warning)
    addTest(TestStatus.Warning)
    addTest(TestStatus.Broken)
    addTest(TestStatus.Broken)
    addTest(TestStatus.Broken)

    val testCounts = dao.getTestCounts()

    testCounts should equal(TestCounts(passed = 1, warning = 2, failed = 3))
  }

  "Getting test counts" should "allow filtering by test name" in transaction { dao ⇒
    val dao = createDao
    def addTest(name: String, status: TestStatus) {
      val testId = dao.ensureTestIsRecorded(F.test(name = name))
      dao.upsertAnalysis(F.analysis(testId, status))
    }
    addTest("test1", TestStatus.Healthy)
    addTest("test2", TestStatus.Warning)
    addTest("test3", TestStatus.Broken)
    addTest("somethingElse", TestStatus.Healthy)

    val testCounts = dao.getTestCounts(nameOpt = Some("test*"))

    testCounts should equal(TestCounts(passed = 1, warning = 1, failed = 1))
  }

  "Getting test counts" should "allow filtering by group" in transaction { dao ⇒
    val dao = createDao
    def addTest(group: String, status: TestStatus) {
      val testId = dao.ensureTestIsRecorded(F.test(groupOpt = Some(group)))
      dao.upsertAnalysis(F.analysis(testId, status))
    }
    addTest("groupOne", TestStatus.Healthy)
    addTest("groupOne", TestStatus.Warning)
    addTest("groupOne", TestStatus.Warning)
    addTest("groupOne", TestStatus.Broken)
    addTest("groupOne", TestStatus.Broken)
    addTest("groupOne", TestStatus.Broken)
    addTest("groupTwo", TestStatus.Healthy)

    val testCounts = dao.getTestCounts(groupOpt = Some("groupO*"))

    testCounts should equal(TestCounts(passed = 1, warning = 2, failed = 3))
  }

  "Getting test counts" should "tolerate unanalysed tests" in transaction { dao ⇒
    val dao = createDao
    val testId = dao.ensureTestIsRecorded(F.test())
    val testCounts = dao.getTestCounts()
    testCounts should equal(TestCounts(passed = 0, warning = 0, failed = 0))
  }

  "Getting test counts" should "exclude ignored tests" in transaction { dao ⇒
    val dao = createDao
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.upsertAnalysis(F.analysis(testId, TestStatus.Healthy))

    val testCounts1 = dao.getTestCounts(ignoredTests = Seq())
    testCounts1 should equal(TestCounts(passed = 1, ignored = 0))

    val testCounts2 = dao.getTestCounts(ignoredTests = Seq(testId))
    testCounts2 should equal(TestCounts(passed = 0, ignored = 1))
  }

  "getEnrichedTest" should "not bring anything back if no test with that id" in transaction { dao ⇒
    val Some(bogusId) = Id.parse[Test]("123")

    dao.getEnrichedTest(bogusId) should equal(None)
  }

  "getEnrichedTest" should "bring back None if it has not been analysed in that configuration" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())

    dao.getEnrichedTest(testId) should equal(None)
  }

  "getIgnoredTests()" should "exclude deleted tests" in transaction { dao ⇒
    val testId = dao.ensureTestIsRecorded(F.test())
    dao.addIgnoredTestConfigurations(Seq(IgnoredTestConfiguration(testId, DummyData.Configuration1)))
    dao.markTestsAsDeleted(Seq(testId), deleted = true)

    dao.getIgnoredTests(DummyData.Configuration1) should equal(Seq())
  }

  "getIgnoredTests()" should "let you filter by name, group and category" in transaction { dao ⇒
    val testId1 = dao.ensureTestIsRecorded(F.test(name = "test1", groupOpt = Some("group")))
    val testId2 = dao.ensureTestIsRecorded(F.test(name = "test2", groupOpt = Some("group2")))
    val testId3 = dao.ensureTestIsRecorded(F.test(name = "test3", groupOpt = Some("group3")))
    for (testId ← Seq(testId1, testId2, testId3))
      dao.addIgnoredTestConfigurations(Seq(IgnoredTestConfiguration(testId, Configuration.Default)))
    dao.addCategories(Seq(F.testCategory(testId3, category = DummyData.Category)))

    dao.getIgnoredTests(Configuration.Default, nameOpt = Some("test1")) should equal(Seq(testId1))
    dao.getIgnoredTests(Configuration.Default, groupOpt = Some("group2")) should equal(Seq(testId2))
    dao.getIgnoredTests(Configuration.Default, categoryOpt = Some(DummyData.Category)) should equal(Seq(testId3))

  }

}