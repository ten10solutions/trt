package com.thetestpeople.trt.model

case class TestCategory(testId: Id[Test], category: String, isUserCategory: Boolean)