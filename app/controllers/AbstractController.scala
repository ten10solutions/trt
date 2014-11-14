package controllers

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.model.jenkins._
import play.Logger
import play.api.mvc._
import viewModel._
import java.net.URI
import play.api.libs.json._
import com.thetestpeople.trt.json.JsonSerializers._

abstract class AbstractController(protected val service: Service) extends Controller {

  protected implicit def globalViewContext: GlobalViewContext = ControllerHelper.globalViewContext(service)

  protected def getDefaultConfiguration: Option[Configuration] = {
    val configurations = service.getConfigurations.sorted
    if (configurations contains Configuration.Default)
      Some(Configuration.Default)
    else
      configurations.headOption
  }

  protected def getFormParameters(parameterName: String)(implicit request: Request[AnyContent]): Seq[String] =
    for {
      requestMap ← request.body.asFormUrlEncoded.toSeq
      values ← requestMap.get(parameterName).toSeq
      value ← values
    } yield value

  protected def getFormParameter(parameterName: String)(implicit request: Request[AnyContent]): Option[String] =
    getFormParameters(parameterName).headOption

  protected def previousUrlOpt(implicit request: Request[AnyContent]): Option[Call] =
    getFormParameter("previousURL").map(url ⇒ new Call("GET", url))

  protected def previousUrlOrDefault(implicit request: Request[AnyContent]): Call =
    previousUrlOpt.getOrElse(routes.Application.index())

}