package controllers

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.model.jenkins._
import play.Logger
import play.api.mvc._
import viewModel._
import java.net.URI

object BatchesController {

  val HideBatchChartThreshold = 1000

}

/**
 * Controller for the Batches screen.
 */
class BatchesController(service: Service) extends AbstractController(service) with HasLogger {

  import BatchesController._

  def batches(jobIdOpt: Option[Id[CiJob]], configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒ BadRequest(errorMessage)
      case Right(pagination)  ⇒ Ok(handleBatches(jobIdOpt, configurationOpt, resultOpt, pagination))
    }
  }

  private def handleBatches(jobIdOpt: Option[Id[CiJob]], configurationOpt: Option[Configuration], resultOpt: Option[Boolean], pagination: Pagination)(implicit request: Request[_]) = {
    val batches = service.getBatches(jobIdOpt, configurationOpt, resultOpt).map(new BatchView(_))
    val jobs = service.getCiJobs()
    val paginationData = pagination.paginationData(batches.size)
    val hideChartInitially = batches.size >= HideBatchChartThreshold
    views.html.batches(batches, jobIdOpt, configurationOpt, resultOpt, jobs, paginationData, hideChartInitially)
  }

  def deleteBatches() = Action { implicit request ⇒
    val batchIds = getSelectedBatchIds(request)

    service.deleteBatches(batchIds)

    val redirectTarget = previousUrlOpt.getOrElse(routes.BatchesController.batches())
    val successMessage = deleteBatchesSuccessMessage(batchIds)
    Redirect(redirectTarget).flashing("success" -> successMessage)
  }

  private def getSelectedBatchIds(implicit request: Request[AnyContent]): Seq[Id[Batch]] =
    getFormParameters("selectedBatch").flatMap(Id.parse[Batch])

  private def deleteBatchesSuccessMessage(batchIds: Seq[Id[Batch]]): String = {
    val batchWord = if (batchIds.size == 1) "batch" else "batches"
    s"Deleted ${batchIds.size} $batchWord"
  }

}