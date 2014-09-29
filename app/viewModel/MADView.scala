package viewModel

import com.thetestpeople.trt.analysis.ExecutionTimeMAD
import com.thetestpeople.trt.utils.DateUtils

case class MADView(mad: ExecutionTimeMAD) {

  def medianExecutionTime: TimeDescription = TimeDescription(mad.medianExecutionTime)

  def deviation: String = DateUtils.describeDuration(mad.medianDeviation)

}