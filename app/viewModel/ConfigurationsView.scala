package viewModel

import org.joda.time.Interval

case class ConfigurationsView(historicalIntervalOpt: Option[Interval]) {

  def timelineOpt: Option[(Long, Long)] = historicalIntervalOpt.map(i ⇒ i.getStartMillis -> i.getEndMillis)

}