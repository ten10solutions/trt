package controllers

import com.thetestpeople.trt.importer._
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.Utils

import play.api.mvc.Action
import viewModel._
import views.html

class ImportLogController(service: Service) extends AbstractController(service) with HasLogger {

  def getCiImportSpec(id: Id[CiImportSpec], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        service.getCiImportSpec(id) match {
          case None ⇒
            NotFound(s"Could not find import spec with id '$id'")
          case Some(spec) ⇒
            val allInfos = getBuildImportInfos(spec)
            val pageInfos = allInfos.drop(pagination.firstItem).take(pagination.pageSize)
            val jobName = service.getCiJobs().find(_.url == spec.jobUrl).map(_.name)
              .getOrElse(spec.jobUrl.toString) // Job name not known until first import completes
            val jobImportInfo = getJobImportInfo(spec)
            val progress = getImportProgressPercent(allInfos)
            val paginationData = pagination.paginationData(allInfos.size)
            Ok(html.ciImportSpec(spec, jobImportInfo, pageInfos, jobName, progress, paginationData))
        }
    }
  }

  private def getImportProgressPercent(allInfos: Seq[CiBuildImportInfo]): Int =
    if (allInfos.isEmpty)
      0
    else {
      val done = allInfos.count(_.importState.done)
      val total = allInfos.size
      val percent = 100.0 * done / total
      percent.toInt
    }

  private def getJobImportInfo(spec: CiImportSpec): CiJobImportInfo = {
    service.getJobImportStatus(spec.id).map { status ⇒
      val importState = status.state match {
        case JobImportState.Complete   ⇒ viewModel.ImportState.Complete
        case JobImportState.InProgress ⇒ viewModel.ImportState.InProgress
        case JobImportState.Errored(_) ⇒ viewModel.ImportState.Errored
        case JobImportState.NotStarted ⇒ viewModel.ImportState.NotStarted
      }
      val summaryOpt = PartialFunction.condOpt(status.state) {
        case JobImportState.Errored(t) ⇒ t.getMessage
      }
      val detailsOpt = PartialFunction.condOpt(status.state) {
        case JobImportState.Errored(t) ⇒ Utils.printStackTrace(t)
      }
      CiJobImportInfo(
        importState = importState,
        updatedAtTimeOpt = Some(status.updatedAt),
        summaryOpt = summaryOpt,
        detailsOpt = detailsOpt)
    }.getOrElse {
      CiJobImportInfo(
        importState = viewModel.ImportState.NotStarted,
        updatedAtTimeOpt = spec.lastCheckedOpt,
        summaryOpt = None)
    }
  }

  private def getBuildImportInfos(spec: CiImportSpec): Seq[CiBuildImportInfo] = {
    val inMemoryStatuses = service.getBuildImportStatuses(spec.id)
    val inMemoryInfos = inMemoryStatuses.map(makeBuildImportInfoFromImportStatus)
    val inMemoryUrls = inMemoryStatuses.map(_.buildUrl).toSet

    def inMemory(build: CiBuild) = inMemoryUrls contains build.buildUrl
    val dbBuilds = service.getCiBuilds(spec.id).filterNot(inMemory)
    val dbInfos = dbBuilds.map(makeBuildImportInfo)

    (dbInfos ++ inMemoryInfos).sortBy(_.buildNumberOpt).reverse
  }

  private def makeBuildImportInfoFromImportStatus(status: CiBuildImportStatus) = {
    val batchIdOpt = PartialFunction.condOpt(status.state) {
      case BuildImportState.Complete(Some(batchId)) ⇒ batchId
    }
    val importState = status.state match {
      case BuildImportState.Complete(_) ⇒ viewModel.ImportState.Complete
      case BuildImportState.InProgress  ⇒ viewModel.ImportState.InProgress
      case BuildImportState.Errored(_)  ⇒ viewModel.ImportState.Errored
      case BuildImportState.NotStarted  ⇒ viewModel.ImportState.NotStarted
    }
    val summaryOpt = PartialFunction.condOpt(status.state) {
      case BuildImportState.Errored(t) ⇒ t.getMessage
    }
    val detailsOpt = PartialFunction.condOpt(status.state) {
      case BuildImportState.Errored(t) ⇒ Utils.printStackTrace(t)
    }
    CiBuildImportInfo(
      buildUrl = status.buildUrl,
      buildNumberOpt = status.buildNumberOpt,
      buildNameOpt = status.buildNameOpt,
      importState = importState,
      updatedAtTime = status.updatedAt,
      batchIdOpt = batchIdOpt,
      summaryOpt = summaryOpt,
      detailsOpt = detailsOpt)
  }

  private def makeBuildImportInfo(build: CiBuild) =
    CiBuildImportInfo(
      buildUrl = build.buildUrl,
      buildNumberOpt = build.buildNumberOpt,
      buildNameOpt = build.buildNameOpt,
      importState = viewModel.ImportState.Complete,
      updatedAtTime = build.importTime,
      batchIdOpt = Some(build.batchId))

}