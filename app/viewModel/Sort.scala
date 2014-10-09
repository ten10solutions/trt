package viewModel

sealed trait Sort

object Sort {

  case object Weather extends Sort
  case object Group extends Sort

  def apply(s: String): Option[Sort] = PartialFunction.condOpt(s) {
    case "Weather" ⇒ Weather
    case "Group"   ⇒ Group
  }

}