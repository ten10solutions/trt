package com.thetestpeople.trt.json

import java.net.URI
import org.joda.time._

import com.thetestpeople.trt.service._
import com.thetestpeople.trt.utils.HasLogger
import com.thetestpeople.trt.model._

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

object JsonSerializers {

  implicit val durationFormat: Format[Duration] = {
    val reads = Reads.LongReads.map(Duration.millis)
    val writes = new Writes[Duration] { def writes(duration: Duration) = JsNumber(duration.getMillis) }
    Format(reads, writes)
  }

  implicit val uriFormat: Format[URI] = {
    val reads = Reads.StringReads.map(new URI(_))
    val writes = new Writes[URI] { def writes(uri: URI) = JsString(uri.toString) }
    Format(reads, writes)
  }

  implicit val configurationFormat: Format[Configuration] = {
    val reads = Reads.StringReads.map(Configuration.apply)
    val writes = new Writes[Configuration] { def writes(config: Configuration) = JsString(config.toString) }
    Format(reads, writes)
  }

  implicit def idFormat[T <: EntityType]: Format[Id[T]] = {
    val reads = Reads.StringReads.map(unlift(Id.parse[T]))
    val writes = new Writes[Id[T]] { def writes(id: Id[T]) = JsString(id.asString) }
    Format(reads, writes)
  }

  implicit val incomingTestFormat: Format[Incoming.Test] = (
    (__ \ "name").format[String] and
    (__ \ "group").formatNullable[String])(Incoming.Test, unlift(Incoming.Test.unapply))

  implicit val incomingExecutionFormat: Format[Incoming.Execution] = (
    (__ \ "test").format[Incoming.Test] and
    (__ \ "passed").format[Boolean] and
    (__ \ "summary").formatNullable[String] and
    (__ \ "log").formatNullable[String] and
    (__ \ "executionTime").formatNullable[DateTime] and
    (__ \ "duration").formatNullable[Duration] and
    (__ \ "configuration").formatNullable[Configuration])(Incoming.Execution, unlift(Incoming.Execution.unapply))

  implicit val incomingBatchFormat: Format[Incoming.Batch] = (
    (__ \ "executions").format[List[Incoming.Execution]] and
    (__ \ "url").formatNullable[URI] and
    (__ \ "name").formatNullable[String] and
    (__ \ "log").formatNullable[String] and
    (__ \ "executionTime").formatNullable[DateTime] and
    (__ \ "duration").formatNullable[Duration] and 
    (__ \ "configuration").formatNullable[Configuration])(Incoming.Batch, unlift(Incoming.Batch.unapply))

  implicit val batchFormat: Format[Batch] = (
    (__ \ "id").format[Id[Batch]] and
    (__ \ "url").formatNullable[URI] and
    (__ \ "executionTime").format[DateTime] and
    (__ \ "duration").formatNullable[Duration] and
    (__ \ "name").formatNullable[String] and
    (__ \ "passed").format[Boolean] and
    (__ \ "totalCount").format[Int] and
    (__ \ "passCount").format[Int] and
    (__ \ "failCount").format[Int] and
    (__ \ "configuration").formatNullable[Configuration])(Batch, unlift(Batch.unapply))

}