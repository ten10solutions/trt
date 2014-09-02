package viewModel

object PaginationData {

  val DefaultWindow = 7

}

case class PaginationData(currentPage: Int, pageSize: Int, maxPage: Int, totalItems: Int) {

  private val window = PaginationData.DefaultWindow
  private val halfWindow = window / 2

  def isRequired = maxPage > 1

  def isLastPage = currentPage == maxPage

  def nextPage = currentPage + 1

  def previousPage = currentPage - 1

  def displayFirstPageSeparately: Boolean = displayedPages.head > 1

  def displayedPages: Seq[Int] = {
    val first = clamp(currentPage - halfWindow, lower = 1, upper = maxPage - window + 1)
    val last = clamp(currentPage + halfWindow, lower = math.min(maxPage, window), upper = maxPage)
    (if (first == 2) 1 else first) to (if (last == maxPage - 1) maxPage else last)
  }

  /**
   * Force the given argument n to be between the given lower and upper bounds (inclusive)
   */
  private def clamp(n: Int, lower: Int, upper: Int) = math.max(lower, math.min(n, upper))

  def displayLastPageSeparately: Boolean = displayedPages.last < maxPage

  def startFrom: Int = (currentPage - 1) * pageSize

}