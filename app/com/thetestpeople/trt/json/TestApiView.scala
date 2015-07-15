package com.thetestpeople.trt.json

import com.thetestpeople.trt.model.Id
import com.thetestpeople.trt.model.Test
import com.thetestpeople.trt.model.TestStatus

/**
 * View of a test which we expose via the JSON API
 */
case class TestApiView(
    id: Id[Test],
    name: String,
    groupOpt: Option[String],
    statusOpt: Option[TestStatus],
    ignored: Boolean)