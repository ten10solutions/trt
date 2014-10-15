package com.thetestpeople.trt.model.jenkins

import com.thetestpeople.trt.model._
import org.joda.time.Duration
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import java.net.URI

case class CiImportSpec(
    id: Id[CiImportSpec] = Id.dummy,
    ciType: CiType,
    jobUrl: URI,
    pollingInterval: Duration,
    importConsoleLog: Boolean,
    lastCheckedOpt: Option[DateTime] = None,
    configurationOpt: Option[Configuration]) extends EntityType {

  def nextCheckDueOpt: Option[DateTime] = lastCheckedOpt.map(_ + pollingInterval)

}
