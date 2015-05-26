package com.thetestpeople.trt.model

trait TestDao {

  /**
   * @return test and analysis for this configuration. Returns None if there are no analysed results for the given test and configuration.
   */
  def getEnrichedTest(id: Id[Test], configuration: Configuration = Configuration.Default): Option[EnrichedTest]

  /**
   * @return the IDs of all tests in the DB (including deleted tests).
   */
  def getTestIds(): Seq[Id[Test]]

  /**
   * @return all tests marked as deleted
   */
  def getDeletedTests(): Seq[Test]

  /**
   * @return all test names matching the given pattern (case-insensitive, allows * wildcards)
   */
  def getTestNames(pattern: String): Seq[String]

  /**
   * @return all groups matching the given pattern (case-insensitive, allows * wildcards)
   */
  def getGroups(pattern: String): Seq[String]

  /**
   * @return all categories matching the given pattern (case-insensitive, allows * wildcards)
   */
  def getCategoryNames(pattern: String): Seq[String]

  /**
   * Get tests and any analysis for the given configuration and filters.
   *
   * Excludes deleted tests
   *
   * @param nameOpt -- if Some(name), filter returned tests to those matching the given name (case insensitive, allows * wildcards)
   * @param groupOpt -- if Some(group), filter returned tests to those matching the given group (case insensitive, allows * wildcards)
   * @param testStatusOpt -- if Some(testStatus), filter returned tests to those matching the given status.
   * @param blackListOpt -- if Some(blackList), exclude any tests in the list from the returned results.
   * @param whiteListOpt -- if Some(whiteList), only include tests from the list in the returned results.
   */
  def getAnalysedTests(
    configuration: Configuration = Configuration.Default,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    blackListOpt: Option[Seq[Id[Test]]] = None,
    whiteListOpt: Option[Seq[Id[Test]]] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None,
    sortBy: SortBy.Test = SortBy.Test.Group()): Seq[EnrichedTest]

  /**
   * Gets test counts for a given configuration (and filters).
   *
   * @param nameOpt -- if Some(name), filter returned tests to those matching the given name (case insensitive, allows * wildcards)
   * @param groupOpt -- if Some(group), filter returned tests to those matching the given group (case insensitive, allows * wildcards)
   * @param ignoredTests -- exclude tests from this set when counting
   *
   * Excludes deleted tests.
   */
  def getTestCounts(
    configuration: Configuration = Configuration.Default,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    ignoredTests: Seq[Id[Test]] = Seq()): TestCounts

  def getTestsById(testIds: Seq[Id[Test]]): Seq[Test]

  /**
   * Checks whether a test is already recorded for the given test's qualifiedName.
   * If not, a new Test record is persisted (the given ID is ignored).
   *
   * @return ID of either an existing or newly-added test.
   */
  def ensureTestIsRecorded(test: Test): Id[Test]

  def markTestsAsDeleted(ids: Seq[Id[Test]], deleted: Boolean = true)

  def upsertAnalysis(analysis: Analysis)

  def setTestComment(id: Id[Test], text: String)

  def deleteTestComment(id: Id[Test])

  def getCategories(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[TestCategory]]

  def getCategories(testId: Id[Test]): Seq[TestCategory] = getCategories(Seq(testId)).getOrElse(testId, Seq())

  def addCategories(categories: Seq[TestCategory])

  def removeCategories(testId: Id[Test], categories: Seq[String])

  def addIgnoredTestConfigurations(ignoredConfigs: Seq[IgnoredTestConfiguration])

  def removeIgnoredTestConfigurations(testIds: Seq[Id[Test]], configuration: Configuration)

  def removeIgnoredTestConfiguration(testId: Id[Test], configuration: Configuration) =
    removeIgnoredTestConfigurations(Seq(testId), configuration)
    
  /**
   * Get ignored tests in a configuration, excluding deleted tests.
   */
  def getIgnoredTests(configuration: Configuration): Seq[Id[Test]]

  def isTestIgnoredInConfiguration(testId: Id[Test], configuration: Configuration): Boolean

  /**
   * For each given testId, if a test exists in the DB, the Map will contain an entry for that test indicating in which
   * configurations it is ignored (this might be empty). If no test with that ID is present in the DB, then there will
   * be no entry for that test in the returned Map.
   */
  def getIgnoredConfigurations(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[Configuration]]

  def getIgnoredConfigurations(testId: Id[Test]): Option[Seq[Configuration]] =
    getIgnoredConfigurations(Seq(testId)).get(testId)

  /**
   * Return the configurations of executions that have been recorded for the given test.
   */
  def getConfigurations(testId: Id[Test]): Seq[Configuration]

}