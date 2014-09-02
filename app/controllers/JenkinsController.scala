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

/**
 * Handle HTTP requests specific to Jenkins functionality
 */
class JenkinsController(service: Service) extends Controller with HasLogger {

  private implicit def globalViewContext: GlobalViewContext = GlobalViewContext(service.getConfigurations())

  import routes.JenkinsController
  import views.html

  def jenkinsImportSpecs() = Action { implicit request ⇒
    logger.debug(s"jenkinsImportSpecs()")
    val specs = service.getJenkinsImportSpecs.map(JenkinsImportSpecView)
    Ok(html.jenkinsImportSpecs(specs))
  }

  def newJenkinsImportSpec() = Action { implicit request ⇒
    logger.debug(s"newJenkinsImportSpec()")
    Ok(html.jenkinsImportSpec(JenkinsImportSpecForm.initial, specOpt = None))
  }

  def deleteJenkinsImportSpec(id: Id[JenkinsImportSpec]) = Action { implicit request ⇒
    logger.debug(s"deleteJenkinsImportSpec($id)")
    val success = service.deleteJenkinsImportSpec(id)
    if (success)
      Redirect(JenkinsController.jenkinsImportSpecs).flashing("success" -> "Deleted import specification")
    else
      NotFound(s"Could not find Jenkins import spec with id '$id'")
  }

  def editJenkinsImportSpec(id: Id[JenkinsImportSpec]) = Action { implicit request ⇒
    logger.debug(s"editJenkinsImportSpec($id)")
    service.getJenkinsImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find Jenkins import spec with id '$id'")
      case Some(spec) ⇒
        val jenkinsImportData = EditableJenkinsImportData.fromSpec(spec)
        val populatedForm = JenkinsImportSpecForm.form.fill(jenkinsImportData)
        Ok(html.jenkinsImportSpec(populatedForm, Some(id)))
    }
  }

  def syncJenkins(id: Id[JenkinsImportSpec]) = Action { implicit request ⇒
    logger.debug(s"syncJenkins($id)")
    if (service.getJenkinsImportSpec(id).isDefined) {
      Future { service.syncJenkins(id) }
      Redirect(JenkinsController.jenkinsImportSpecs).flashing("success" -> "Sync has been triggered")
    } else
      NotFound(s"Could not find Jenkins import spec with id '$id'")
  }

  def createJenkinsImportSpec() = Action { implicit request ⇒
    logger.debug(s"createJenkinsImportSpec()")
    JenkinsImportSpecForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.jenkinsImportSpec(formWithErrors, None)),
      jenkinsImport ⇒ {
        service.newJenkinsImportSpec(jenkinsImport.newSpec)
        Redirect(JenkinsController.jenkinsImportSpecs).flashing("success" -> "Created new import specification")
      })
  }

  def updateJenkinsImportSpec(id: Id[JenkinsImportSpec]) = Action { implicit request ⇒
    logger.debug(s"updateJenkinsImportSpec($id)")
    service.getJenkinsImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find Jenkins import spec with id '$id'")
      case Some(spec) ⇒
        JenkinsImportSpecForm.form.bindFromRequest.fold(
          formWithErrors ⇒
            BadRequest(html.jenkinsImportSpec(formWithErrors, Some(id))),
          jenkinsImport ⇒ {
            service.updateJenkinsImportSpec(jenkinsImport.updatedSpec(spec))
            Redirect(JenkinsController.jenkinsImportSpecs).flashing("success" -> "Updated import specification")
          })
    }
  }

  private def getJenkinsConfigurationForm: Form[EditableJenkinsConfiguration] = {
    val editableConfig = EditableJenkinsConfiguration(service.getJenkinsConfiguration)
    JenkinsConfigurationForm.form.fill(editableConfig)
  }

  def auth() = Action { implicit request ⇒
    logger.debug(s"auth()")
    Ok(html.jenkinsAuth(getJenkinsConfigurationForm))
  }

  def reruns() = Action { implicit request ⇒
    logger.debug(s"reruns()")
    Ok(html.jenkinsReruns(getJenkinsConfigurationForm))
  }

  def updateAuth() = Action { implicit request ⇒
    logger.debug(s"updateAuth()")
    JenkinsConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒ {
        logger.debug("updateAuth() errors: " + formWithErrors.errorsAsJson)
        BadRequest(html.jenkinsAuth(formWithErrors))
      },
      jenkinsConfiguration ⇒ {
        service.updateJenkinsConfiguration(jenkinsConfiguration.asJenkinsConfiguration)
        Redirect(JenkinsController.auth).flashing("success" -> "Updated configuration")
      })
  }

  def updateReruns() = Action { implicit request ⇒
    logger.debug(s"updateReruns()")
    JenkinsConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.jenkinsReruns(formWithErrors)),
      jenkinsConfiguration ⇒ {
        service.updateJenkinsConfiguration(jenkinsConfiguration.asJenkinsConfiguration)
        Redirect(JenkinsController.reruns).flashing("success" -> "Updated configuration")
      })
  }

  private def previousUrlOpt(implicit request: Request[AnyContent]): Option[Call] =
    for {
      requestMap ← request.body.asFormUrlEncoded
      values ← requestMap.get("previousURL")
      previousUrl ← values.headOption
    } yield new Call("GET", previousUrl)

  def rerunSelectedTests() = Action { implicit request ⇒
    val selectedTestIds: List[Id[Test]] = getSelectedTestIds(request)
    logger.debug(s"rerunSelectedTests(${selectedTestIds.mkString(",")})")
    rerunTests(selectedTestIds)
  }

  private def rerunTests(testIds: List[Id[Test]])(implicit request: Request[AnyContent]) = {
    val triggerResult = service.rerunTests(testIds)

    val redirectTarget = previousUrlOpt.getOrElse(routes.Application.tests())

    triggerResult match {
      case TriggerResult.Success(jobUrl) ⇒
        Redirect(redirectTarget).flashing(
          "success" -> s"Triggered Jenkins build for ${testIds.size} ${if (testIds.size == 1) "test" else "tests"}",
          "link" -> jobUrl.toString)
      case TriggerResult.AuthenticationProblem(message) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"Could not trigger Jenkins build because of an authentication problem: $message. Check your Jenkins configuration:",
          "link" -> routes.JenkinsController.auth.url)
      case TriggerResult.ParameterProblem(param) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"Could not trigger Jenkins build because of a problem with parameter '$param'. Check your Jenkins configuration:",
          "link" -> routes.JenkinsController.reruns.url)
      case TriggerResult.OtherProblem(message, _) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"There was a problem triggering Jenkins build: $message")
    }
  }

  private def getSelectedTestIds(request: Request[AnyContent]): List[Id[Test]] =
    for {
      requestMap ← request.body.asFormUrlEncoded.toList
      selectedIds ← requestMap.get("selectedTest").toList
      idString ← selectedIds
      id ← Id.parse[Test](idString)
    } yield id

  def rerunTest(testId: Id[Test]) = Action { implicit request ⇒
    logger.debug(s"rerunTest($testId)")
    rerunTests(List(testId))
  }

}
