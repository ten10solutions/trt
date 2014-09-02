package com.thetestpeople.trt.model

import scala.PartialFunction.condOpt

sealed trait TestStatus

object TestStatus {

  case object Pass extends TestStatus
  case object Warn extends TestStatus
  case object Fail extends TestStatus

  def parse(s: String): TestStatus = unapply(s).getOrElse(
    throw new RuntimeException(s"Unknown status type '$s'"))

  def unapply(s: String): Option[TestStatus] =
    condOpt(s) {
      case "Pass" ⇒ Pass
      case "Warn" ⇒ Warn
      case "Fail" ⇒ Fail
    }

}