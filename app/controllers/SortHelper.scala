package controllers

import com.thetestpeople.trt.model.SortBy

import viewModel.Sort

object SortHelper {

  def getTestSortBy(sortOpt: Option[Sort], descendingOpt: Option[Boolean]): SortBy.Test = sortOpt match {
    case Some(Sort.Weather)             ⇒ SortBy.Test.Weather(descendingOpt getOrElse false)
    case Some(Sort.Group)               ⇒ SortBy.Test.Group(descendingOpt getOrElse false)
    case Some(Sort.Name)                ⇒ SortBy.Test.Name(descendingOpt getOrElse false)
    case Some(Sort.Duration)            ⇒ SortBy.Test.Duration(descendingOpt getOrElse false)
    case Some(Sort.ConsecutiveFailures) ⇒ SortBy.Test.ConsecutiveFailures(descendingOpt getOrElse false)
    case Some(Sort.StartedFailing)      ⇒ SortBy.Test.StartedFailing(descendingOpt getOrElse false)
    case Some(Sort.LastPassed)          ⇒ SortBy.Test.LastPassed(descendingOpt getOrElse false)
    case Some(Sort.LastFailed)          ⇒ SortBy.Test.LastFailed(descendingOpt getOrElse false)
    case None                           ⇒ SortBy.Test.Group(descending = false)
  }

}