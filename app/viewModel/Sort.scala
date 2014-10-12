package viewModel

sealed trait Sort

object Sort {

  case object Weather extends Sort
  case object Group extends Sort
  case object Name extends Sort
  case object Duration extends Sort
  case object ConsecutiveFailures extends Sort
  case object StartedFailing extends Sort
  case object LastPassed extends Sort
  case object LastFailed extends Sort

  def apply(s: String): Option[Sort] = PartialFunction.condOpt(s) {
    case "Weather"             ⇒ Weather
    case "Group"               ⇒ Group
    case "Name"                ⇒ Name
    case "Duration"            ⇒ Duration
    case "ConsecutiveFailures" ⇒ ConsecutiveFailures
    case "StartedFailing"      ⇒ StartedFailing
    case "LastPassed"          ⇒ LastPassed
    case "LastFailed"          ⇒ LastFailed
  }

}