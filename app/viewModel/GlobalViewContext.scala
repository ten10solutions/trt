package viewModel

import com.thetestpeople.trt.model.Configuration

/**
 * Data that might be needed by all the views
 */
case class GlobalViewContext(applicationName: String, projectNameOpt: Option[String], configurations: Seq[Configuration], hasExecutions: Boolean) {

  def noData = !hasExecutions

  def singleConfigurationMode = configurations.size == 1

  def multipleConfigurationMode = configurations.size > 1

  
}