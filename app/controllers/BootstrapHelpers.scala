package controllers

import views.html.helper.FieldConstructor

object BootstrapHelpers {

  implicit val fieldConstructor = FieldConstructor(views.html.bootstrap3FieldConstructorTemplate.f)

}