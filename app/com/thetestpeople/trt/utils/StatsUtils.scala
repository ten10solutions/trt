package com.thetestpeople.trt.utils

object StatsUtils {

  def median(items: Seq[Double]): Option[Double] = {
    val sortedItems = items.sorted
    val count = items.size
    if (count == 0)
      None
    else if (count % 2 == 0)
      Some((sortedItems(count / 2) + sortedItems(count / 2 - 1)) / 2.0)
    else
      Some(sortedItems(count / 2))
  }

}