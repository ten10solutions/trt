package com.thetestpeople.trt.model

import java.net.URI
import com.thetestpeople.trt.model.jenkins._
import org.joda.time.Duration


trait Dao extends ExecutionDao with CiDao with BatchDao with TestDao {

  /**
   * Run the given block within a transaction
   */
  def transaction[T](block: ⇒ T): T

  def getSystemConfiguration(): SystemConfiguration

  def updateSystemConfiguration(newConfig: SystemConfiguration)

  def getConfigurations(): Seq[Configuration]

  /**
   * Return the configurations of executions that have been recorded of the given test.
   */
  def getConfigurations(testId: Id[Test]): Seq[Configuration]

  def getCategories(testIds: Seq[Id[Test]]): Map[Id[Test], Seq[TestCategory]]

  def getCategories(testId: Id[Test]): Seq[TestCategory] = getCategories(Seq(testId)).getOrElse(testId, Seq())
  
  def addCategories(categories: Seq[TestCategory])

  def removeCategories(testId: Id[Test], categories: Seq[String])

}
