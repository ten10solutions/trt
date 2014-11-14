package controllers

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.Test
import play.api.mvc.AnyContent
import play.api.mvc.Request
import viewModel.GlobalViewContext
import com.thetestpeople.trt.service.Service
import com.thetestpeople.trt.utils.Utils

object ControllerHelper {

  def globalViewContext(service: Service): GlobalViewContext = {
    val configurations = service.getConfigurations()
    val projectNameOpt = service.getSystemConfiguration().projectNameOpt
    GlobalViewContext(projectNameOpt, configurations, service.hasExecutions())
  }

}