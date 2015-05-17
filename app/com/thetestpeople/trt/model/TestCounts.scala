package com.thetestpeople.trt.model

case class TestCounts(
    passed: Int = 0, 
    warning: Int = 0, 
    failed: Int = 0,
    ignored: Int = 0) {

  lazy val total = passed + warning + failed + ignored

  def countFor(testStatusOpt: Option[TestStatus]): Int = testStatusOpt match {
    case Some(TestStatus.Healthy) ⇒ passed
    case Some(TestStatus.Warning) ⇒ warning
    case Some(TestStatus.Broken)  ⇒ failed
    case None                     ⇒ total
  }

}