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
import play.api.libs.json._
import com.thetestpeople.trt.json.JsonSerializers._

/**
 * Controller for Batch and Batch Log screen
 */
class BatchController(service: Service) extends AbstractController(service) with HasLogger {

  def batch(batchId: Id[Batch], passedFilterOpt: Option[Boolean], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        handleBatch(batchId, passedFilterOpt, pagination) match {
          case None       ⇒ NotFound(s"Could not find batch with id '$batchId'")
          case Some(html) ⇒ Ok(html)
        }
    }
  }

  private def handleBatch(batchId: Id[Batch], passedFilterOpt: Option[Boolean], pagination: Pagination)(implicit request: Request[_]) =
    service.getBatchAndExecutions(batchId, passedFilterOpt) map {
      case EnrichedBatch(batch, executions, logOpt, importSpecIdOpt, commentOpt) ⇒
        val batchView = new BatchView(batch, executions, logOpt, importSpecIdOpt, commentOpt)
        val paginationData = pagination.paginationData(executions.size)
        views.html.batch(batchView, passedFilterOpt, service.canRerun, paginationData)
    }

  def setBatchComment(batchId: Id[Batch]) = Action { implicit request ⇒
    getFormParameter("text") match {
      case Some(text) ⇒
        val result = service.setBatchComment(batchId, text)
        if (result)
          Redirect(routes.BatchController.batch(batchId)).flashing("success" -> "Comment updated.")
        else
          NotFound(s"Could not find batch with id '$batchId'")
      case None ⇒
        BadRequest("No 'text' parameter provided'")
    }
  }

  def deleteBatch(batchId: Id[Batch]) = Action { implicit request ⇒
    val batchIds = Seq(batchId)
    service.deleteBatches(batchIds)

    val successMessage = deleteBatchesSuccessMessage(batchIds)
    Redirect(routes.BatchesController.batches()).flashing("success" -> successMessage)
  }

  private def deleteBatchesSuccessMessage(batchIds: Seq[Id[Batch]]): String = {
    val batchWord = if (batchIds.size == 1) "batch" else "batches"
    s"Deleted ${batchIds.size} $batchWord"
  }

  def batchLog(batchId: Id[Batch]) = Action { implicit request ⇒
    service.getBatchAndExecutions(batchId, None) match {
      case None ⇒
        NotFound(s"Could not find batch with id '$batchId'")
      case Some(EnrichedBatch(batch, executions, logOpt, importSpecIdOpt, commentOpt)) ⇒
        logOpt match {
          case Some(log) ⇒
            val batchView = new BatchView(batch, Seq(), logOpt, importSpecIdOpt, commentOpt)
            Ok(views.html.batchLog(batchView, log))
          case None ⇒
            NotFound(s"Batch $batchId does not have an associated log recorded")
        }
    }
  }

}