package viewModel

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils.DateUtils

abstract class AbstractExecutionView(execution: AbstractExecution) {

  def id: Id[_]

  def executionTime: TimeDescription = TimeDescription(execution.executionTime)

  def durationOpt: Option[String] = execution.durationOpt.map(DateUtils.describeDuration)

  def durationSecondsOpt: Option[Double] = execution.durationOpt.map(_.getMillis / 1000.0)

  def passFailText: String = if (passed) "Passed" else "Failed"

  def passFailIcon = TickIcons.icon(execution.passed)

  def passed = execution.passed

  def failed = execution.failed

  def epochMillis = execution.executionTime.getMillis

}