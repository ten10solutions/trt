package com.thetestpeople.trt.model

import org.joda.time.DateTime
import org.joda.time.Duration
import java.net.URI

/**
 * Record of the execution of a batch of test cases
 */
case class Batch(
  id: Id[Batch] = Id.dummy,
  urlOpt: Option[URI],
  executionTime: DateTime,
  durationOpt: Option[Duration],
  nameOpt: Option[String],
  passed: Boolean,
  totalCount: Int,
  passCount: Int,
  failCount: Int) extends AbstractExecution with EntityType
