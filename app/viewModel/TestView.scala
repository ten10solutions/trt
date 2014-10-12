package viewModel

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._
import org.joda.time.LocalDate
import com.thetestpeople.trt.utils.DateUtils

case class WeatherInfo(weather: Double) {
  
  def iconPath: String = WeatherIcons.weatherIcon(weather)
  
  def passRate: String = "Pass rate: " + (weather * 100).toInt + "%"
}

case class TestView(testAndAnalysis: TestAndAnalysis) extends HasTestName {

  private val test = testAndAnalysis.test

  def testName = test.qualifiedName

  def id = test.id

  def deleted = testAndAnalysis.test.deleted

  def ballIconOpt: Option[String] = testAndAnalysis.analysisOpt.map(_.status).map(BallIcons.icon)

  def weatherInfoOpt: Option[WeatherInfo] = testAndAnalysis.analysisOpt.map(_.weather).map(WeatherInfo)

  def consecutiveFailuresOpt: Option[Int] =
    for {
      analysis ← testAndAnalysis.analysisOpt
      consecutiveFailures = analysis.consecutiveFailures
      if consecutiveFailures > 0
    } yield consecutiveFailures

  def lastExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastExecutionIdOpt; time ← lastExecutedTimeOpt) yield (id, time)
  def lastExecutionIdOpt: Option[Id[Execution]] =
    testAndAnalysis.analysisOpt.map(_.lastExecutionId)
  def lastExecutedTimeOpt: Option[TimeDescription] =
    testAndAnalysis.analysisOpt.map(_.lastExecutionTime).map(TimeDescription)

  def lastPassedExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastPassedExecutionIdOpt; time ← lastPassedTimeOpt) yield (id, time)
  def lastPassedExecutionIdOpt: Option[Id[Execution]] =
    testAndAnalysis.analysisOpt.flatMap(_.lastPassedExecutionIdOpt)
  def lastPassedTimeOpt: Option[TimeDescription] =
    testAndAnalysis.analysisOpt.flatMap(_.lastPassedTimeOpt).map(TimeDescription)

  def lastFailedExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastFailedExecutionIdOpt; time ← lastFailedTimeOpt) yield (id, time)
  def lastFailedExecutionIdOpt: Option[Id[Execution]] =
    testAndAnalysis.analysisOpt.flatMap(_.lastFailedExecutionIdOpt)
  def lastFailedTimeOpt: Option[TimeDescription] =
    testAndAnalysis.analysisOpt.flatMap(_.lastFailedTimeOpt).map(TimeDescription)

  def failingSinceOpt: Option[TimeDescription] =
    for {
      analysis ← testAndAnalysis.analysisOpt
      failingSince ← analysis.failingSinceOpt
    } yield TimeDescription(failingSince)

  def commentOpt: Option[String] = testAndAnalysis.commentOpt

  def medianDurationOpt: Option[String] =
    testAndAnalysis.analysisOpt.flatMap(_.medianDurationOpt).map(DateUtils.describeDuration)

}