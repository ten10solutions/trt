package com.thetestpeople.trt.webdriver.screens

import scala.annotation.tailrec
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.WaitUtils._
import com.thetestpeople.trt.webdriver.screens.RichSelenium._

class WaitedTooLongException(message: String = null) extends RuntimeException(message)

case class WaitSpec(timeout: Int = 30, retryWait: Int = 3)

object WaitUtils {

  def waitUntil[T](p: ⇒ Boolean)(implicit waitSpec: WaitSpec = WaitSpec()) {
    val start = System.currentTimeMillis
    while (!p) {
      if (System.currentTimeMillis - start > waitSpec.timeout * 1000)
        throw new WaitedTooLongException()
      Thread.sleep(waitSpec.retryWait * 1000)
    }
  }

  def waitForOneOf[T](pf: PartialFunction[WebDriver, T])(implicit webDriver: WebDriver, waitSpec: WaitSpec = WaitSpec()): T = {
    val start = System.currentTimeMillis
    @tailrec
    def loop(): T = pf.lift.apply(webDriver) match {
      case Some(result) ⇒
        result
      case None ⇒
        if (System.currentTimeMillis - start > waitSpec.timeout * 1000)
          throw new WaitedTooLongException()
        else {
          Thread.sleep(waitSpec.retryWait * 1000)
          loop()
        }
    }
    loop()
  }

}
case class DisplayedAndEnabled(locator: By) {

  def unapply(webDriver: WebDriver): Option[WebElement] = webDriver.findImmediateDisplayedAndEnabled(locator)

}
