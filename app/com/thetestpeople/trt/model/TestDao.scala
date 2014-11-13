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
   */
  def getAnalysedTests(
    configuration: Configuration = Configuration.Default,
    testStatusOpt: Option[TestStatus] = None,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None,
    startingFrom: Int = 0,
    limitOpt: Option[Int] = None,
    sortBy: SortBy.Test = SortBy.Test.Group()): Seq[EnrichedTest]

  /**
   * Gets test counts for all configurations.
   *
   * Excludes deleted tests
   *
   * @return a map of configuration to test counts for that configuration
   */
  def getTestCountsByConfiguration(): Map[Configuration, TestCounts]

  /**
   * Gets test counts for a given configuration (and filters).
   *
   * @param nameOpt -- if Some(name), filter returned tests to those matching the given name (case insensitive, allows * wildcards)
   * @param groupOpt -- if Some(group), filter returned tests to those matching the given group (case insensitive, allows * wildcards)
   *
   * Excludes deleted tests
   */
  def getTestCounts(
    configuration: Configuration = Configuration.Default,
    nameOpt: Option[String] = None,
    groupOpt: Option[String] = None,
    categoryOpt: Option[String] = None): TestCounts

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

}