package com.thetestpeople.trt.model

import scala.PartialFunction.condOpt

sealed trait TestStatus

object TestStatus {

  case object Healthy extends TestStatus
  case object Warning extends TestStatus
  case object Broken extends TestStatus

  def parseOld(s: String): TestStatus =
    s match {
      case "Pass" ⇒ Healthy
      case "Warn" ⇒ Warning
      case "Fail" ⇒ Broken
    }

  def oldLabel(status: TestStatus) = status match {
    case Healthy ⇒ "Pass"
    case Warning ⇒ "Warn"
    case Broken  ⇒ "Fail"
  }

  def parse(s: String): TestStatus = unapply(s).getOrElse(
    throw new RuntimeException(s"Unknown status type '$s'"))

  def unapply(s: String): Option[TestStatus] =
    condOpt(s) {
      case "Healthy" ⇒ Healthy
      case "Warning" ⇒ Warning
      case "Broken"  ⇒ Broken
    }

  def identifier(status: TestStatus) = status match {
    case Healthy ⇒ "Healthy"
    case Warning ⇒ "Warning"
    case Broken  ⇒ "Broken"
  }
}