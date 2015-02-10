package com.thetestpeople.trt.importer.jenkins

import java.net.URI
import java.net.URLDecoder
import scala.PartialFunction.condOpt

case class AxisValue(axis: String, value: String)

case class MatrixConfiguration(axisValues: Seq[AxisValue])

object MatrixJobUrlParser {

  private val UrlRegex = """^/job/[^/]+/([^/]+)/.*$""".r

  /**
   * From a URL like:
   * https://jenkins.example.com/job/foo/Browser_Type=desktop,label=linux/10
   *
   * Extract the Browser_Type=desktop and label=linux pairs.
   */
  def getConfigurations(jobUrl: URI): Option[MatrixConfiguration] = condOpt(jobUrl.getPath) {
    case UrlRegex(axisValuesString) ⇒
      MatrixConfiguration(axisValuesString.split(",").toSeq.flatMap(getAxisValue))
  }

  private def getAxisValue(axisValueString: String): Option[AxisValue] =
    condOpt(axisValueString.split("=").toSeq) {
      case Seq(axis, value) ⇒ AxisValue(axis, value)
    }

}