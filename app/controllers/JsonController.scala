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

object JsonController {

  val MaxBatchJsonSize = 1024 * 1024 * 200

}

class JsonController(service: Service, adminService: AdminService) extends Controller with HasLogger {

  def addBatch = Action(parse.json(maxLength = JsonController.MaxBatchJsonSize)) { implicit request ⇒
    logger.debug(s"addBatch")
    request.body.validate[Incoming.Batch].map { batch ⇒
      val batchId = service.addBatch(batch)
      Ok(JsString(batchId.toString))
    }.recoverTotal(e ⇒
      BadRequest("Error parsing batch JSON: " + JsError.toFlatJson(e)))
  }

  def getBatches = Action { implicit request ⇒
    val batches = service.getBatches()
    Ok(Json.toJson(batches))
  }

  def deleteAll() = Action { implicit request ⇒
    logger.debug(s"deleteAll()")
    adminService.deleteAll()
    Ok(JsString("Deleted all data"))
  }

}