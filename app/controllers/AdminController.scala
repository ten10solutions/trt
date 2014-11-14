package controllers

import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils.HasLogger

import play.api.mvc._
import viewModel._

class AdminController(service: Service, adminService: AdminService) extends AbstractController(service) with HasLogger {

  def admin() = Action { implicit request ⇒
    Ok(views.html.admin())
  }

  def deleteAll() = Action { implicit request ⇒
    adminService.deleteAll()
    Redirect(routes.AdminController.admin).flashing("success" -> "All data deleted")
  }

  def analyseAll() = Action { implicit request ⇒
    adminService.analyseAll()
    Redirect(routes.AdminController.admin).flashing("success" -> "Analysis of all tests scheduled")
  }
  
}