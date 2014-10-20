package viewModel

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.CiImportSpec
import java.net.URI
import org.joda.time.DateTime
import com.thetestpeople.trt.model.Batch

case class JenkinsBuildImportInfo(
    buildUrl: URI,
    buildNumberOpt: Option[Int],
    importState: ImportState,
    updatedAtTime: DateTime,
    batchIdOpt: Option[Id[Batch]] = None,
    summaryOpt: Option[String] = None,
    detailsOpt: Option[String] = None) {

  def updatedAt = TimeDescription(updatedAtTime)

}

case class JenkinsJobImportInfo(
    importState: ImportState, 
    updatedAtTimeOpt: Option[DateTime], 
    summaryOpt: Option[String] = None,
    detailsOpt: Option[String] = None) {

  def updatedAtOpt: Option[TimeDescription] = updatedAtTimeOpt.map(TimeDescription)

}

sealed abstract class ImportState(val description: String, val done: Boolean) {

  override def toString = description
  
}

object ImportState {

  case object InProgress extends ImportState("In progress", done = false)
  case object Complete extends ImportState("Complete", done = true)
  case object Errored extends ImportState("Errored", done = true)
  case object NotStarted extends ImportState("Not started", done = false)

}
