package viewModel

import com.thetestpeople.trt.model.jenkins._
import controllers.jenkins.JenkinsImportSpecForm
import org.joda.time.Duration
import java.net.URI
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.model.CIType

object EditableJenkinsImportData {

  def fromSpec(spec: JenkinsImportSpec): EditableJenkinsImportData =
    EditableJenkinsImportData(
      jobUrl = spec.jobUrl,
      pollingInterval = spec.pollingInterval,
      importConsoleLog = spec.importConsoleLog,
      configurationOpt = spec.configurationOpt)

}

case class EditableJenkinsImportData(
    jobUrl: URI,
    pollingInterval: Duration,
    importConsoleLog: Boolean,
    configurationOpt: Option[Configuration]) {

  def updatedSpec(spec: JenkinsImportSpec): JenkinsImportSpec =
    spec.copy(jobUrl = jobUrl,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      configurationOpt = configurationOpt)

  def newSpec(): JenkinsImportSpec =
    JenkinsImportSpec(
      jobUrl = jobUrl,
      ciType = CIType.Jenkins,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      configurationOpt = configurationOpt)
}