package com.thetestpeople.trt.model

case class TestAndAnalysis(test: Test, analysisOpt: Option[Analysis] = None, commentOpt: Option[String] = None) {

  def id = test.id

  def name = test.name

}
