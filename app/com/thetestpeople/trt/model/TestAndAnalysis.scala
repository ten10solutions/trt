package com.thetestpeople.trt.model

case class TestAndAnalysis(test: Test, analysisOpt: Option[Analysis]) {

  def id = test.id

  def name = test.name

}
