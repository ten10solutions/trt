package viewModel

import com.thetestpeople.trt.utils.DateUtils
import com.thetestpeople.trt.model._

class BatchView(batch: Batch, _executions: List[EnrichedExecution] = Nil, logOpt: Option[String] = None) extends AbstractExecutionView(batch) {

  def id = batch.id

  val executions: List[ExecutionView] = _executions.sortBy(_.qualifiedName).map(ExecutionView)

  def urlOpt: Option[String] = batch.urlOpt.map(_.toString)

  def nameOpt: Option[String] = batch.nameOpt

  def hasLog: Boolean = logOpt.isDefined

  def passCount: Int = batch.passCount

  def failCount: Int = batch.failCount

  def totalCount: Int = batch.totalCount

  def passPercent = if (totalCount == 0) 0 else 100.0 * passCount / totalCount

  def failPercent = if (totalCount == 0) 0 else 100.0 * failCount / totalCount

}