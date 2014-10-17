package com.thetestpeople.trt.jenkins.importer

import com.thetestpeople.trt.model.CiType
import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.jenkins.CiDao
import com.thetestpeople.trt.model.jenkins.CiImportSpec
import com.thetestpeople.trt.service.BatchRecorder
import com.thetestpeople.trt.service.Clock
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.utils.http.Http

class CiImporter(
    clock: Clock,
    http: Http,
    dao: CiDao,
    importStatusManager: CiImportStatusManager,
    batchRecorder: BatchRecorder) extends HasLogger {

  import dao.transaction

  def importBuilds(specId: Id[CiImportSpec]) {
    val specOpt = transaction(dao.getCiImportSpec(specId))
    val spec = specOpt.getOrElse {
      logger.warn(s"No import spec found $specId, skipping")
      return
    }
    logger.debug(s"Examining ${spec.jobUrl} for new builds")
    importStatusManager.importStarted(spec.id, spec.jobUrl)
    try {
      spec.ciType match {
        case CiType.Jenkins  ⇒ jenkinsImporter.importBuilds(spec)
        case CiType.TeamCity ⇒ teamCityImporter.importBuilds(spec)
        case t               ⇒ logger.warn(s"Unknown CI type $t for spec $specId, skipping")
      }
      importStatusManager.importComplete(spec.id)
    } catch {
      case e: Exception ⇒
        logger.error(s"Problem importing from ${spec.jobUrl}", e)
        importStatusManager.importErrored(spec.id, e)
    }
  }

  private def jenkinsImporter = new JenkinsImporter(clock, http, dao, importStatusManager, batchRecorder)
  private def teamCityImporter = new TeamCityImporter(clock, http, dao, importStatusManager, batchRecorder)

}