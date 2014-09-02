package viewModel

import com.github.nscala_time.time.Imports._
import com.thetestpeople.trt.analysis.HistoricalTestCountsTimeline

case class HistoricalTestCountsTimelineView(timeline: HistoricalTestCountsTimeline) {

  private def counts = timeline.counts
  
  val passes: List[(Long, Int)] = counts.map(counts ⇒ (counts.when.getMillis, counts.testCounts.passed))

  val warnings: List[(Long, Int)] = counts.map(counts ⇒ (counts.when.getMillis, counts.testCounts.warning))

  val failures: List[(Long, Int)] = counts.map(counts ⇒ (counts.when.getMillis, counts.testCounts.failed))

}