package viewModel

object WeatherIcons {

  def weatherIcon(weather: Double): String = weather match {
    case n if n < 0.20 ⇒ "weather/health-00to19.png"
    case n if n < 0.40 ⇒ "weather/health-20to39.png"
    case n if n < 0.60 ⇒ "weather/health-40to59.png"
    case n if n < 0.80 ⇒ "weather/health-60to79.png"
    case n             ⇒ "weather/health-80plus.png"
  }

}