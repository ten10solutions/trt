package viewModel

import com.thetestpeople.trt.model.jenkins.CiImportSpec
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.utils.DateUtils

case class CiImportSpecView(spec: CiImportSpec, inProgress: Boolean) {

  def id = spec.id

  def jobName: String = {
    val path = jobUrl.getPath
    val lastJobIndex = path.lastIndexOf("/job/")
    if (lastJobIndex == -1)
      jobUrl.toString
    else
      path.drop(lastJobIndex + "/job".length)
  }

  def jobUrl = spec.jobUrl

  def pollingInterval = DateUtils.describeDuration(spec.pollingInterval)

  def importConsoleLog: Boolean = spec.importConsoleLog

  def lastCheckedOpt: Option[String] = spec.lastCheckedOpt.map(DateUtils.describeRelative)

  def configuration = spec.configurationOpt.getOrElse("")

}