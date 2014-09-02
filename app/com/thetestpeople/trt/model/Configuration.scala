package com.thetestpeople.trt.model

object Configuration {

  val Default = Configuration("Default")

  implicit val configurationOrdering: Ordering[Configuration] = Ordering.by(_.configuration)

}

/**
 * A name given to a distinct context under which tests are run, e.g. with a particular browser, or against a particular
 * test environment etc.
 */
case class Configuration(configuration: String) {

  override def toString = configuration

}