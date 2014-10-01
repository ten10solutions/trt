package controllers

import viewModel.PaginationData

object Pagination {

  val DefaultPageSize = 20

  def validate(pageOpt: Option[Int], pageSizeOpt: Option[Int], defaultPageSize: Int = DefaultPageSize): Either[String, Pagination] = {
    val page = pageOpt.getOrElse(1)
    if (page < 1)
      Left(s"Page must be > 0, but was '$page'")
    else {
      val pageSize = pageSizeOpt.getOrElse(defaultPageSize)
      if (pageSize < 0)
        Left(s"Page size must be > 0, but was '$pageSize'")
      else
        Right(Pagination(page, pageSize))
    }
  }

}

/**
 * @param page -- page number, 1-indexed
 */
case class Pagination(page: Int, pageSize: Int) {

  def pageIndex = page - 1

  def firstItem = pageIndex * pageSize

  private def lastPage(totalItems: Int) = math.ceil(totalItems / pageSize.toDouble).toInt

  def paginationData(totalItems: Int) = PaginationData(page, pageSize, lastPage(totalItems), totalItems)

}