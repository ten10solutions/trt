package controllers

import java.net.URI
import org.joda.time._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.model._
import com.thetestpeople.trt.json.JsonSerializers._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import com.thetestpeople.trt.utils.Utils

object JsonController {

  val MaxBatchJsonSize = 1024 * 1024 * 200

}

class JsonController(service: Service, adminService: AdminService) extends Controller with HasLogger {

  def addBatch = Action(parse.json(maxLength = JsonController.MaxBatchJsonSize)) { implicit request ⇒
    Utils.time("JsonController.addBatch()") {
      request.body.validate[Incoming.Batch].map { batch ⇒
        val batchId = service.addBatch(batch)
        Ok(JsString(batchId.toString))
      }.recoverTotal(e ⇒
        BadRequest("Error parsing batch JSON: " + JsError.toFlatJson(e)))
    }
  }

  def addExecutions(batchId: Id[Batch]) = Action(parse.json(maxLength = JsonController.MaxBatchJsonSize)) { implicit request ⇒
    request.body.validate[Seq[Incoming.Execution]].map { executions ⇒
      val batchFound = service.addExecutionsToBatch(batchId, executions)
      if (batchFound)
        Ok("Executions added to batch")
      else
        NotFound(s"Could not find batch with id batchId")
    }.recoverTotal(e ⇒
      BadRequest("Error parsing executions JSON: " + JsError.toFlatJson(e)))
  }

  def completeBatch(batchId: Id[Batch]) = Action(parse.json) { implicit request ⇒
    request.body.validate[Incoming.BatchCompleteMessage].map { batchCompleteMessage ⇒
      val batchFound = service.completeBatch(batchId, batchCompleteMessage.durationOpt)
      if (batchFound)
        Ok("Batch completed")
      else
        NotFound(s"Could not find batch with id batchId")
    }.recoverTotal(e ⇒
      BadRequest("Error parsing JSON: " + JsError.toFlatJson(e)))
  }

  def getBatches = Action { implicit request ⇒
    val batches = service.getBatches()
    Ok(Json.toJson(batches))
  }

  def deleteAll() = Action { implicit request ⇒
    adminService.deleteAll()
    Ok(JsString("Deleted all data"))
  }

}