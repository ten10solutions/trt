package controllers

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.Test
import play.api.mvc.AnyContent
import play.api.mvc.Request
import viewModel.GlobalViewContext
import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.Utils

object ControllerHelper {

  def getSelectedTestIds(request: Request[AnyContent]): Seq[Id[Test]] =
    for {
      requestMap ← request.body.asFormUrlEncoded.toSeq
      selectedIds ← requestMap.get("selectedTest").toSeq
      idString ← selectedIds
      id ← Id.parse[Test](idString)
    } yield id

  def globalViewContext(service: Service): GlobalViewContext = {
    val configurations = service.getConfigurations()
    val projectNameOpt = service.getSystemConfiguration().projectNameOpt
    GlobalViewContext(projectNameOpt, configurations, service.hasExecutions())
  }

}