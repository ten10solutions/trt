package com.thetestpeople.trt.analysis

import com.thetestpeople.trt.model.ExecutionLite
import org.joda.time.LocalDate
import com.thetestpeople.trt.model.Configuration
import scala.collection.immutable.SortedMap
import com.github.nscala_time.time.Imports._

case class ExecutionVolume(countsByDate: SortedMap[LocalDate, Int])

case class ExecutionVolumeAnalysisResult(all: ExecutionVolume, byConfiguration: Map[Configuration, ExecutionVolume]) {

  def getExecutionVolume(configurationOpt: Option[Configuration]): Option[ExecutionVolume] = configurationOpt match {
    case None                ⇒ Some(all)
    case Some(configuration) ⇒ byConfiguration.get(configuration)
  }

}

class ExecutionVolumeAnalyser extends ExecutionAnalyser[ExecutionVolumeAnalysisResult] {

  private var allCounts: SortedMap[LocalDate, Int] = SortedMap()
  private var countsByConfiguration: Map[Configuration, SortedMap[LocalDate, Int]] = Map()

  def executionGroup(executionGroup: ExecutionGroup) {
    for (execution ← executionGroup.executions) {
      val date = execution.executionTime.toLocalDate
      allCounts += date -> (allCounts.getOrElse(date, 0) + 1)
      val configuration = execution.configuration
      var counts = countsByConfiguration.getOrElse(configuration, SortedMap[LocalDate, Int]())
      counts += date -> (counts.getOrElse(date, 0) + 1)
      countsByConfiguration += configuration -> counts
    }
  }

  def finalise(): ExecutionVolumeAnalysisResult = {
    ExecutionVolumeAnalysisResult(
      all = ExecutionVolume(allCounts),
      byConfiguration = countsByConfiguration.map { case (configuration, counts) ⇒ configuration -> ExecutionVolume(counts) })
  }
}