package controllers

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.model.jenkins._
import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.jenkins.trigger.TriggerResult
import controllers.jenkins._
import viewModel._
import scala.concurrent.Future
import play.Logger
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.data.Form
import com.thetestpeople.trt.importer.jenkins._
import com.thetestpeople.trt.importer._
import routes.ImportSpecController
import views.html
import com.thetestpeople.trt.utils.Utils
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

class ImportSpecController(service: Service) extends AbstractController(service) with HasLogger {

  def ciImportSpecs() = Action { implicit request ⇒
    val specs = service.getCiImportSpecs.map(makeView).sortBy(_.jobUrl).toList
    Ok(html.ciImportSpecs(specs))
  }

  private def makeView(spec: CiImportSpec): CiImportSpecView = {
    val inProgress = PartialFunction.cond(service.getJobImportStatus(spec.id)) {
      case Some(CiJobImportStatus(_, _, JobImportState.InProgress)) ⇒ true
    }
    CiImportSpecView(spec, inProgress)
  }

  def syncCiImport(id: Id[CiImportSpec]) = Action { implicit request ⇒
    if (service.getCiImportSpec(id).isDefined) {
      service.syncCiImport(id)
      Redirect(routes.ImportLogController.getCiImportSpec(id)).flashing("success" -> "Sync has been triggered")
    } else
      NotFound(s"Could not find CI import spec with id '$id'")
  }

  def deleteCiImportSpec(id: Id[CiImportSpec]) = Action { implicit request ⇒
    val success = service.deleteCiImportSpec(id)
    if (success)
      Redirect(ImportSpecController.ciImportSpecs).flashing("success" -> "Deleted import specification")
    else
      NotFound(s"Could not find import spec with id '$id'")
  }

  def newCiImportSpec() = Action { implicit request ⇒
    Ok(html.editCiImportSpec(CiImportSpecForm.initial, specOpt = None))
  }

  def editCiImportSpec(id: Id[CiImportSpec]) = Action { implicit request ⇒
    service.getCiImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find import spec with id '$id'")
      case Some(spec) ⇒
        val importSpec = EditableImportSpec.fromSpec(spec)
        val populatedForm = CiImportSpecForm.form.fill(importSpec)
        Ok(html.editCiImportSpec(populatedForm, Some(id)))
    }
  }

  def createCiImportSpec() = Action { implicit request ⇒
    CiImportSpecForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.editCiImportSpec(formWithErrors, None)),
      editableSpec ⇒ {
        val specId = service.newCiImportSpec(editableSpec.newSpec)
        Redirect(routes.ImportLogController.getCiImportSpec(specId)).flashing("success" -> "Created new import specification")
      })
  }

  def updateCiImportSpec(id: Id[CiImportSpec]) = Action { implicit request ⇒
    service.getCiImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find import spec with id '$id'")
      case Some(spec) ⇒
        CiImportSpecForm.form.bindFromRequest.fold(
          formWithErrors ⇒
            BadRequest(html.editCiImportSpec(formWithErrors, Some(id))),
          editableSpec ⇒ {
            service.updateCiImportSpec(editableSpec.applyEdits(spec))
            Redirect(ImportSpecController.ciImportSpecs).flashing("success" -> "Updated import specification")
          })
    }
  }
}
