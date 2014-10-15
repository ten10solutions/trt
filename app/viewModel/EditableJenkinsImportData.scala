package viewModel

import com.thetestpeople.trt.model.jenkins._
import controllers.jenkins.CiImportSpecForm
import org.joda.time.Duration
import java.net.URI
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.model.CiType

object EditableJenkinsImportData {

  def fromSpec(spec: CiImportSpec): EditableJenkinsImportData =
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

  def updatedSpec(spec: CiImportSpec): CiImportSpec =
    spec.copy(jobUrl = jobUrl,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      configurationOpt = configurationOpt)

  def newSpec(): CiImportSpec =
    CiImportSpec(
      jobUrl = jobUrl,
      ciType = CiType.Jenkins,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      configurationOpt = configurationOpt)
}