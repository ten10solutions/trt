package com.thetestpeople.trt.model

object SortBy {

  sealed trait Test {
    def descending: Boolean
  }

  object Test {
    case class Weather(descending: Boolean = false) extends SortBy.Test
    case class Group(descending: Boolean = false) extends SortBy.Test
    case class Name(descending: Boolean = false) extends SortBy.Test
    case class Duration(descending: Boolean = false) extends SortBy.Test
    case class ConsecutiveFailures(descending: Boolean = false) extends SortBy.Test
    case class StartedFailing(descending: Boolean = false) extends SortBy.Test
    case class LastPassed(descending: Boolean = false) extends SortBy.Test
    case class LastFailed(descending: Boolean = false) extends SortBy.Test
  }

}