package viewModel

import com.thetestpeople.trt.model._
import com.github.nscala_time.time.Imports._
import org.joda.time.LocalDate
import com.thetestpeople.trt.utils.DateUtils

case class WeatherInfo(weather: Double) {

  def iconPath: String = WeatherIcons.weatherIcon(weather)

  def passRate: String = "Pass rate: " + (weather * 100).toInt + "%"
}

case class TestView(
    enrichedTest: EnrichedTest,
    categories: Seq[String] = Seq(),
    isIgnoredInConfiguration: Boolean = false) extends HasTestName {

  private val test = enrichedTest.test

  def testName = test.qualifiedName

  def id = test.id

  def deleted = enrichedTest.test.deleted

  def ballIconOpt: Option[String] =
    if (isIgnoredInConfiguration)
      Some(BallIcons.GreyBall)
    else
      enrichedTest.statusOpt.map(BallIcons.icon)

  def statusOpt = enrichedTest.statusOpt
      
  def weatherInfoOpt: Option[WeatherInfo] = enrichedTest.analysisOpt.map(_.weather).map(WeatherInfo)

  def consecutiveFailuresOpt: Option[Int] =
    for {
      analysis ← enrichedTest.analysisOpt
      consecutiveFailures = analysis.consecutiveFailures
      if consecutiveFailures > 0
    } yield consecutiveFailures

  def lastExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastExecutionIdOpt; time ← lastExecutedTimeOpt) yield (id, time)
  def lastExecutionIdOpt: Option[Id[Execution]] =
    enrichedTest.analysisOpt.map(_.lastExecutionId)
  def lastExecutedTimeOpt: Option[TimeDescription] =
    enrichedTest.analysisOpt.map(_.lastExecutionTime).map(TimeDescription)

  def lastPassedExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastPassedExecutionIdOpt; time ← lastPassedTimeOpt) yield (id, time)
  def lastPassedExecutionIdOpt: Option[Id[Execution]] =
    enrichedTest.analysisOpt.flatMap(_.lastPassedExecutionIdOpt)
  def lastPassedTimeOpt: Option[TimeDescription] =
    enrichedTest.analysisOpt.flatMap(_.lastPassedTimeOpt).map(TimeDescription)

  def lastFailedExecutionOpt: Option[(Id[Execution], TimeDescription)] =
    for (id ← lastFailedExecutionIdOpt; time ← lastFailedTimeOpt) yield (id, time)
  def lastFailedExecutionIdOpt: Option[Id[Execution]] =
    enrichedTest.analysisOpt.flatMap(_.lastFailedExecutionIdOpt)
  def lastFailedTimeOpt: Option[TimeDescription] =
    enrichedTest.analysisOpt.flatMap(_.lastFailedTimeOpt).map(TimeDescription)

  def failingSinceOpt: Option[TimeDescription] =
    for {
      analysis ← enrichedTest.analysisOpt
      failingSince ← analysis.failingSinceOpt
    } yield TimeDescription(failingSince)

  def commentOpt: Option[String] = enrichedTest.commentOpt

  def medianDurationOpt: Option[String] =
    enrichedTest.analysisOpt.flatMap(_.medianDurationOpt).map(DateUtils.describeDuration)

}