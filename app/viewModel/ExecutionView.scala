package viewModel

import com.thetestpeople.trt.utils.DateUtils
import com.thetestpeople.trt.model._

case class ExecutionView(execution: EnrichedExecution) extends AbstractExecutionView(execution.execution) with HasTestName {

  def id = execution.execution.id

  def testName = execution.qualifiedName

  def batchNameOpt = execution.batchNameOpt

  def batchName: String = batchNameOpt.getOrElse("Batch")

  def summaryOpt: Option[AbbreviableText] = execution.summaryOpt.map(AbbreviableText)

  def batchId = execution.batchId

  def testId = execution.testId

  def logOpt = execution.logOpt

  def configuration: Configuration = execution.configuration
}