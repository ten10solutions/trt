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
import com.thetestpeople.trt.jenkins.importer.JenkinsImportStatusManager
import com.thetestpeople.trt.jenkins.importer.JobImportState
import com.thetestpeople.trt.jenkins.importer.JenkinsBuildImportStatus
import com.thetestpeople.trt.jenkins.importer.BuildImportState
import com.thetestpeople.trt.jenkins.importer.JobImportState
import com.thetestpeople.trt.jenkins.importer.JenkinsJobImportStatus

/**
 * Handle HTTP requests specific to Jenkins functionality
 */
class JenkinsController(service: Service) extends Controller with HasLogger {

  private implicit def globalViewContext: GlobalViewContext = ControllerHelper.globalViewContext(service)

  import routes.JenkinsController
  import views.html

  def jenkinsImportSpecs() = Action { implicit request ⇒
    logger.debug(s"jenkinsImportSpecs()")
    val specs = service.getJenkinsImportSpecs.map(makeView).sortBy(_.jobUrl)
    Ok(html.jenkinsImportSpecs(specs))
  }

  private def makeView(spec: JenkinsImportSpec): JenkinsImportSpecView = {
    val inProgress = PartialFunction.cond(service.getJobImportStatus(spec.id)) {
      case Some(JenkinsJobImportStatus(_, _, JobImportState.InProgress)) ⇒ true
    }
    JenkinsImportSpecView(spec, inProgress)
  }

  def newJenkinsImportSpec() = Action { implicit request ⇒
    logger.debug(s"newJenkinsImportSpec()")
    Ok(html.editJenkinsImportSpec(JenkinsImportSpecForm.initial, specOpt = None))
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
        Ok(html.editJenkinsImportSpec(populatedForm, Some(id)))
    }
  }

  def getJenkinsImportSpec(id: Id[JenkinsImportSpec]) = Action { implicit request ⇒
    logger.debug(s"getJenkinsImportSpec($id)")
    service.getJenkinsImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find Jenkins import spec with id '$id'")
      case Some(spec) ⇒
        val allInfos = getBuildImportInfos(spec)
        val jobName = service.getJenkinsJobs().find(_.url == spec.jobUrl).map(_.name)
          .getOrElse(spec.jobUrl.toString) // Job name not known until first import completes
        val jobImportInfo = getJobImportInfo(spec)
        val progress = getJenkinsImportProgressPercent(allInfos)
        Ok(html.jenkinsImportSpec(spec, jobImportInfo, allInfos, jobName, progress))
    }
  }

  def createJenkinsImportSpec() = Action { implicit request ⇒
    logger.debug(s"createJenkinsImportSpec()")
    JenkinsImportSpecForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.editJenkinsImportSpec(formWithErrors, None)),
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
            BadRequest(html.editJenkinsImportSpec(formWithErrors, Some(id))),
          jenkinsImport ⇒ {
            service.updateJenkinsImportSpec(jenkinsImport.updatedSpec(spec))
            Redirect(JenkinsController.jenkinsImportSpecs).flashing("success" -> "Updated import specification")
          })
    }
  }

  private def getJenkinsImportProgressPercent(allInfos: Seq[JenkinsBuildImportInfo]): Int =
    if (allInfos.isEmpty)
      0
    else {
      val done = allInfos.count(_.importState.done)
      val total = allInfos.size
      val percent = 100.0 * done / total
      percent.toInt
    }

  private def getJobImportInfo(spec: JenkinsImportSpec): JenkinsJobImportInfo = {
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
        case JobImportState.Errored(t) ⇒ printStackTrace(t)
      }
      JenkinsJobImportInfo(
        importState = importState,
        updatedAtTimeOpt = Some(status.updatedAt),
        summaryOpt = summaryOpt,
        detailsOpt = detailsOpt)
    }.getOrElse {
      JenkinsJobImportInfo(
        importState = viewModel.ImportState.NotStarted,
        updatedAtTimeOpt = spec.lastCheckedOpt,
        summaryOpt = None)
    }
  }

  private def printStackTrace(t: Throwable): String = {
    val stringWriter = new java.io.StringWriter
    t.printStackTrace(new java.io.PrintWriter(stringWriter))
    stringWriter.toString
  }

  private def getBuildImportInfos(spec: JenkinsImportSpec): Seq[JenkinsBuildImportInfo] = {
    val inMemoryStatuses = service.getBuildImportStatuses(spec.id)
    val inMemoryInfos = inMemoryStatuses.map(makeBuildImportInfoFromImportStatus)
    val inMemoryUrls = inMemoryStatuses.map(_.buildUrl).toSet

    def inMemory(build: JenkinsBuild) = inMemoryUrls contains build.buildUrl
    val dbBuilds = service.getJenkinsBuilds(spec.jobUrl).filterNot(inMemory)
    val dbInfos = dbBuilds.map(makeBuildImportInfo)

    (dbInfos ++ inMemoryInfos).sortBy(_.buildNumber).reverse
  }

  private def makeBuildImportInfoFromImportStatus(status: JenkinsBuildImportStatus) = {
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
      case BuildImportState.Errored(t) ⇒ printStackTrace(t)
    }
    JenkinsBuildImportInfo(
      buildUrl = status.buildUrl,
      buildNumber = status.buildNumber,
      importState = importState,
      updatedAtTime = status.updatedAt,
      batchIdOpt = batchIdOpt,
      summaryOpt = summaryOpt,
      detailsOpt = detailsOpt)
  }

  private def makeBuildImportInfo(build: JenkinsBuild) =
    JenkinsBuildImportInfo(
      buildUrl = build.buildUrl,
      buildNumber = build.buildNumber,
      importState = viewModel.ImportState.Complete,
      updatedAtTime = build.importTime,
      batchIdOpt = Some(build.batchId))

  def syncJenkins(id: Id[JenkinsImportSpec]) = Action { implicit request ⇒
    logger.debug(s"syncJenkins($id)")
    if (service.getJenkinsImportSpec(id).isDefined) {
      service.syncJenkins(id)
      Redirect(JenkinsController.getJenkinsImportSpec(id)).flashing("success" -> "Sync has been triggered")
    } else
      NotFound(s"Could not find Jenkins import spec with id '$id'")
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
    val selectedTestIds: List[Id[Test]] = ControllerHelper.getSelectedTestIds(request)
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

  def rerunTest(testId: Id[Test]) = Action { implicit request ⇒
    logger.debug(s"rerunTest($testId)")
    rerunTests(List(testId))
  }

}
