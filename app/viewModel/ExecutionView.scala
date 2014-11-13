package viewModel

import com.thetestpeople.trt.utils.DateUtils
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service.ExecutionAndFragment

object ExecutionView {

  def fromExecutionAndFragment(execution: ExecutionAndFragment) = ExecutionView(execution.execution, Some(execution.fragment))

}

case class ExecutionView(execution: EnrichedExecution, fragmentOpt: Option[String] = None) extends AbstractExecutionView(execution.execution) with HasTestName {

  def id = execution.execution.id

  def testName = execution.qualifiedName

  def batchNameOpt = execution.batchNameOpt

  def batchName: String = batchNameOpt.getOrElse("Batch")

  def summaryOpt: Option[AbbreviableText] = execution.summaryOpt.map(AbbreviableText)

  def batchId = execution.batchId

  def testId = execution.testId

  def logOpt = execution.logOpt

  def commentOpt = execution.commentOpt

  def configuration: Configuration = execution.configuration

}