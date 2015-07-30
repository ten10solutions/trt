package viewModel

import com.thetestpeople.trt.utils.StringUtils._

case class AbbreviableName(full: String) {

  def abbreviated = abbreviate(maxLength = 20)

  def abbreviate(maxLength: Int): String =
    if (full.size <= maxLength)
      full
    else {
      val withDottedPrefix = abbreviateDottedPrefix(full)
      if (withDottedPrefix.size <= maxLength)
        withDottedPrefix
      else {
        val withoutPrefix = full.split("\\.").lastOption.getOrElse(full)
        ellipsisiseMiddle(withoutPrefix, maxLength)
      }
    }

  override def toString = abbreviated
}