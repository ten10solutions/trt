package viewModel

import com.thetestpeople.trt.model.jenkins.JenkinsImportSpec
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.DateUtils

case class JenkinsImportSpecView(spec: JenkinsImportSpec) {

  def id = spec.id

  def jobUrl = spec.jobUrl

  def pollingInterval = DateUtils.describeDuration(spec.pollingInterval)

  def importConsoleLog: Boolean = spec.importConsoleLog

  def lastCheckedOpt: Option[String] = spec.lastCheckedOpt.map(DateUtils.describeRelative)

  def configuration = spec.configurationOpt.getOrElse("")
}