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
import com.thetestpeople.trt.json.TestApiView

object ApiController {

  val MaxBatchJsonSize = 1024 * 1024 * 200

}

class ApiController(service: Service, adminService: AdminService) extends AbstractController(service) with HasLogger {

  def addBatch = Action(parse.json(maxLength = ApiController.MaxBatchJsonSize)) { implicit request ⇒
    Utils.time("ApiController.addBatch()") {
      request.body.validate[Incoming.Batch].map { batch ⇒
        val batchId = service.addBatch(batch)
        Ok(JsString(batchId.toString))
      }.recoverTotal(e ⇒
        BadRequest("Error parsing batch JSON: " + JsError.toFlatJson(e)))
    }
  }

  def addExecutions(batchId: Id[Batch]) = Action(parse.json(maxLength = ApiController.MaxBatchJsonSize)) { implicit request ⇒
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

  def getTests(configurationOpt: Option[Configuration], testStatusOpt: Option[TestStatus]) = Action { implicit request ⇒
    configurationOpt.orElse(getDefaultConfiguration) match {
      case None ⇒
        Ok(Json.toJson(Seq[String]()))
      case Some(configuration) ⇒
        val TestsInfo(tests, testCounts, ignoredTests) =
          service.getTests(configuration = configuration, testStatusOpt = testStatusOpt)
        Ok(Json.toJson(tests.map(makeView(ignoredTests.toSet))))
    }
  }

  private def makeView(ignoredTests: Set[Id[Test]])(test: EnrichedTest): TestApiView =
    TestApiView(id = test.id,
      name = test.name,
      groupOpt = test.groupOpt,
      statusOpt = test.analysisOpt.map(_.status),
      ignored = ignoredTests.contains(test.id))

  def deleteAll() = Action { implicit request ⇒
    adminService.deleteAll()
    Ok(JsString("Deleted all data"))
  }

}