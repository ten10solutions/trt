package viewModel

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._
import org.joda.time.LocalDate

class TestView(testInfo: TestAndAnalysis) extends HasTestName {

  private val test = testInfo.test

  def testName = test.qualifiedName

  def id = test.id

  def deleted = testInfo.test.deleted

  def ballIconOpt: Option[String] = testInfo.analysisOpt.map(_.status).map(BallIcons.icon)

  def weatherIconOpt: Option[String] = testInfo.analysisOpt.map(_.weather).map(WeatherIcons.weatherIcon)

  def consecutiveFailuresOpt: Option[Int] =
    for {
      analysis ← testInfo.analysisOpt
      consecutiveFailures = analysis.consecutiveFailures
      if consecutiveFailures > 0
    } yield consecutiveFailures

  def lastExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastExecutionIdOpt; time ← lastExecutedTimeOpt) yield (id, time)
  def lastExecutionIdOpt: Option[Id[Execution]] =
    testInfo.analysisOpt.map(_.lastExecutionId)
  def lastExecutedTimeOpt: Option[TimeDescription] =
    testInfo.analysisOpt.map(_.lastExecutionTime).map(TimeDescription)

  def lastPassedExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastPassedExecutionIdOpt; time ← lastPassedTimeOpt) yield (id, time)
  def lastPassedExecutionIdOpt: Option[Id[Execution]] =
    testInfo.analysisOpt.flatMap(_.lastPassedExecutionIdOpt)
  def lastPassedTimeOpt: Option[TimeDescription] =
    testInfo.analysisOpt.flatMap(_.lastPassedTimeOpt).map(TimeDescription)

  def lastFailedExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastFailedExecutionIdOpt; time ← lastFailedTimeOpt) yield (id, time)
  def lastFailedExecutionIdOpt: Option[Id[Execution]] =
    testInfo.analysisOpt.flatMap(_.lastFailedExecutionIdOpt)
  def lastFailedTimeOpt: Option[TimeDescription] =
    testInfo.analysisOpt.flatMap(_.lastFailedTimeOpt).map(TimeDescription)

  def failingSinceOpt: Option[TimeDescription] =
    for {
      analysis ← testInfo.analysisOpt
      failingSince ← analysis.failingSinceOpt
    } yield TimeDescription(failingSince)

  def commentOpt: Option[String] = testInfo.commentOpt

}