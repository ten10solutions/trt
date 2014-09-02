package viewModel

import com.thetestpeople.trt.model._

case class TestsSummaryView(configuration: Configuration, testCounts: TestCounts) {

  val passCount = testCounts.passed

  val failCount = testCounts.failed

  val warnCount = testCounts.warning

  val totalCount = testCounts.total

  val overallStatus: TestStatus =
    if (failCount > 0)
      TestStatus.Fail
    else if (warnCount > 0)
      TestStatus.Warn
    else
      TestStatus.Pass

  def ballIcon: String = BallIcons.icon(overallStatus)

}