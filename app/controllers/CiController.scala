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

/**
 * Handle HTTP requests specific to Jenkins functionality
 */
class CiController(service: Service) extends Controller with HasLogger {

  private implicit def globalViewContext: GlobalViewContext = ControllerHelper.globalViewContext(service)

  import routes.CiController
  import views.html

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

  def newCiImportSpec() = Action { implicit request ⇒
    Ok(html.editCiImportSpec(CiImportSpecForm.initial, specOpt = None))
  }

  def deleteCiImportSpec(id: Id[CiImportSpec]) = Action { implicit request ⇒
    val success = service.deleteCiImportSpec(id)
    if (success)
      Redirect(CiController.ciImportSpecs).flashing("success" -> "Deleted import specification")
    else
      NotFound(s"Could not find Jenkins import spec with id '$id'")
  }

  def editCiImportSpec(id: Id[CiImportSpec]) = Action { implicit request ⇒
    service.getCiImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find Jenkins import spec with id '$id'")
      case Some(spec) ⇒
        val jenkinsImportData = EditableImportSpec.fromSpec(spec)
        val populatedForm = CiImportSpecForm.form.fill(jenkinsImportData)
        Ok(html.editCiImportSpec(populatedForm, Some(id)))
    }
  }

  def getCiImportSpec(id: Id[CiImportSpec], pageOpt: Option[Int], pageSizeOpt: Option[Int]) = Action { implicit request ⇒
    Pagination.validate(pageOpt, pageSizeOpt) match {
      case Left(errorMessage) ⇒
        BadRequest(errorMessage)
      case Right(pagination) ⇒
        service.getCiImportSpec(id) match {
          case None ⇒
            NotFound(s"Could not find Jenkins import spec with id '$id'")
          case Some(spec) ⇒
            val allInfos = getBuildImportInfos(spec)
            val pageInfos = allInfos.drop(pagination.firstItem).take(pagination.pageSize)
            val jobName = service.getCiJobs().find(_.url == spec.jobUrl).map(_.name)
              .getOrElse(spec.jobUrl.toString) // Job name not known until first import completes
            val jobImportInfo = getJobImportInfo(spec)
            val progress = getJenkinsImportProgressPercent(allInfos)
            val paginationData = pagination.paginationData(allInfos.size)
            Ok(html.ciImportSpec(spec, jobImportInfo, pageInfos, jobName, progress, paginationData))
        }
    }
  }

  def createCiImportSpec() = Action { implicit request ⇒
    CiImportSpecForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.editCiImportSpec(formWithErrors, None)),
      editableSpec ⇒ {
        val specId = service.newCiImportSpec(editableSpec.newSpec)
        Redirect(CiController.getCiImportSpec(specId)).flashing("success" -> "Created new import specification")
      })
  }

  def updateCiImportSpec(id: Id[CiImportSpec]) = Action { implicit request ⇒
    service.getCiImportSpec(id) match {
      case None ⇒
        NotFound(s"Could not find Jenkins import spec with id '$id'")
      case Some(spec) ⇒
        CiImportSpecForm.form.bindFromRequest.fold(
          formWithErrors ⇒
            BadRequest(html.editCiImportSpec(formWithErrors, Some(id))),
          editableSpec ⇒ {
            service.updateCiImportSpec(editableSpec.applyEdits(spec))
            Redirect(CiController.ciImportSpecs).flashing("success" -> "Updated import specification")
          })
    }
  }

  private def getJenkinsImportProgressPercent(allInfos: Seq[CiBuildImportInfo]): Int =
    if (allInfos.isEmpty)
      0
    else {
      val done = allInfos.count(_.importState.done)
      val total = allInfos.size
      val percent = 100.0 * done / total
      percent.toInt
    }

  private def getJobImportInfo(spec: CiImportSpec): JenkinsJobImportInfo = {
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
      case BuildImportState.Errored(t) ⇒ printStackTrace(t)
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

  def syncCiImport(id: Id[CiImportSpec]) = Action { implicit request ⇒
    if (service.getCiImportSpec(id).isDefined) {
      service.syncCiImport(id)
      Redirect(CiController.getCiImportSpec(id)).flashing("success" -> "Sync has been triggered")
    } else
      NotFound(s"Could not find Jenkins import spec with id '$id'")
  }

  private def getJenkinsConfigurationForm: Form[EditableJenkinsConfiguration] = {
    val editableConfig = EditableJenkinsConfiguration(service.getJenkinsConfiguration)
    JenkinsConfigurationForm.form.fill(editableConfig)
  }

  private def getTeamCityConfigurationForm: Form[EditableTeamCityConfiguration] = {
    val editableConfig = EditableTeamCityConfiguration(service.getTeamCityConfiguration)
    TeamCityConfigurationForm.form.fill(editableConfig)
  }

  def auth() = Action { implicit request ⇒
    Ok(html.jenkinsAuth(getJenkinsConfigurationForm))
  }

  def reruns() = Action { implicit request ⇒
    Ok(html.jenkinsReruns(getJenkinsConfigurationForm))
  }

  def updateAuth() = Action { implicit request ⇒
    JenkinsConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒ {
        logger.debug("updateAuth() errors: " + formWithErrors.errorsAsJson)
        BadRequest(html.jenkinsAuth(formWithErrors))
      },
      jenkinsConfiguration ⇒ {
        service.updateJenkinsConfiguration(jenkinsConfiguration.asJenkinsConfiguration)
        Redirect(CiController.auth).flashing("success" -> "Updated configuration")
      })
  }

  def updateReruns() = Action { implicit request ⇒
    JenkinsConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒
        BadRequest(html.jenkinsReruns(formWithErrors)),
      jenkinsConfiguration ⇒ {
        service.updateJenkinsConfiguration(jenkinsConfiguration.asJenkinsConfiguration)
        Redirect(CiController.reruns).flashing("success" -> "Updated configuration")
      })
  }

  def teamCityConfig() = Action { implicit request ⇒
    Ok(html.teamCityConfig(getTeamCityConfigurationForm))
  }

  def updateTeamCityConfig() = Action { implicit request ⇒
    TeamCityConfigurationForm.form.bindFromRequest().fold(
      formWithErrors ⇒ {
        logger.debug("updateTeamCityConfig() errors: " + formWithErrors.errorsAsJson)
        BadRequest(html.teamCityConfig(formWithErrors))
      },
      configuration ⇒ {
        val newConfig = configuration.asTeamCityConfiguration
        service.updateTeamCityConfiguration(newConfig)
        logger.debug(s"New TeamCity config: $newConfig")
        Redirect(CiController.teamCityConfig).flashing("success" -> "Updated configuration")
      })
  }

  private def previousUrlOpt(implicit request: Request[AnyContent]): Option[Call] =
    for {
      requestMap ← request.body.asFormUrlEncoded
      values ← requestMap.get("previousURL")
      previousUrl ← values.headOption
    } yield new Call("GET", previousUrl)

  def rerunSelectedTests() = Action { implicit request ⇒
    val selectedTestIds: Seq[Id[Test]] = ControllerHelper.getSelectedTestIds(request)
    logger.debug(s"rerunSelectedTests(${selectedTestIds.mkString(",")})")
    rerunTests(selectedTestIds)
  }

  private def rerunTests(testIds: Seq[Id[Test]])(implicit request: Request[AnyContent]) = {
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
          "link" -> routes.CiController.auth.url)
      case TriggerResult.ParameterProblem(param) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"Could not trigger Jenkins build because of a problem with parameter '$param'. Check your Jenkins configuration:",
          "link" -> routes.CiController.reruns.url)
      case TriggerResult.OtherProblem(message, _) ⇒
        Redirect(redirectTarget).flashing(
          "error" -> s"There was a problem triggering Jenkins build: $message")
    }
  }

  def rerunTest(testId: Id[Test]) = Action { implicit request ⇒
    rerunTests(List(testId))
  }

}
