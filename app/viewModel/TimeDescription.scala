package viewModel

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.thetestpeople.trt.model._
import com.thetestpeople.trt.utils.DateUtils

case class TimeDescription(time: DateTime) {

  def relative: String = DateUtils.describeRelative(time)

  def absolute: String = DateTimeFormat.mediumDateTime.print(time)

}
