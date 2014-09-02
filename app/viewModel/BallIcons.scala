package viewModel

import com.thetestpeople.trt.model.TestStatus

object BallIcons {

  def icon(passed: Boolean) = if (passed) PassBall else FailBall

  def icon(status: TestStatus) = status match {
    case TestStatus.Pass ⇒ BallIcons.PassBall
    case TestStatus.Warn ⇒ BallIcons.WarnBall
    case TestStatus.Fail ⇒ BallIcons.FailBall
  }

  val PassBall = "balls/green.png"
  val WarnBall = "balls/yellow.png"
  val FailBall = "balls/red.png"

}