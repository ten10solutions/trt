package viewModel

import com.thetestpeople.trt.model.jenkins._
import controllers.jenkins.CiImportSpecForm
import org.joda.time.Duration
import java.net.URI
import com.thetestpeople.trt.model.Configuration
import com.thetestpeople.trt.model.CiType

object EditableImportSpec {

  def fromSpec(spec: CiImportSpec): EditableImportSpec =
    EditableImportSpec(
      jobUrl = spec.jobUrl,
      pollingInterval = spec.pollingInterval,
      importConsoleLog = spec.importConsoleLog,
      configurationOpt = spec.configurationOpt)

}

case class EditableImportSpec(
    jobUrl: URI,
    pollingInterval: Duration,
    importConsoleLog: Boolean,
    configurationOpt: Option[Configuration]) {

  def applyEdits(spec: CiImportSpec): CiImportSpec =
    spec.copy(jobUrl = jobUrl,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      configurationOpt = configurationOpt)

  def newSpec(): CiImportSpec =
    CiImportSpec(
      jobUrl = jobUrl,
      ciType = CiType.inferCiType(jobUrl).get,
      pollingInterval = pollingInterval,
      importConsoleLog = importConsoleLog,
      configurationOpt = configurationOpt)
}