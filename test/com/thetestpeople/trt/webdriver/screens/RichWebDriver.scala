package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.support.ui.Wait
import com.google.common.base.{ Function ⇒ GFunction }
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.TimeoutException
import scala.collection.JavaConverters._

object RichSelenium {

  implicit class RichWebElement(element: WebElement) {

    def isDisplayedAndEnabled = element.isDisplayed && element.isEnabled

    def id: String = element.getAttribute("id")

    def findImmediate(locator: By): Option[WebElement] =
      try
        Some(element.findElement(locator))
      catch {
        case e: NoSuchElementException ⇒ None
      }

    def findElements_(locator: By): List[WebElement] =
      element.findElements(locator).asScala.toList

  }

  implicit class RichWebDriver(webDriver: WebDriver) {

    def findElements_(locator: By): List[WebElement] =
      webDriver.findElements(locator).asScala.toList

    def isDisplayedAndEnabled(locator: By) = findImmediateDisplayedAndEnabled(locator).isDefined

    def findImmediateDisplayedAndEnabled(locator: By): Option[WebElement] =
      findImmediate(locator).filter(_.isDisplayedAndEnabled)

    def findImmediate(locator: By): Option[WebElement] =
      try
        Some(webDriver.findElement(locator))
      catch {
        case e: NoSuchElementException ⇒ None
      }

    def webDriverWait(timeoutInSeconds: Long = 20) = new WebDriverWait(webDriver, timeoutInSeconds)

    def waitFor(locator: By): WebElement =
      webDriverWait().waitUntil(s"${locator.toString} is present") {
        findImmediate(locator)
      }

    def waitForDisplayedAndEnabled(locator: By): WebElement =
      webDriverWait().waitUntil(s"${locator.toString} is displayed and enabled") {
        findImmediate(locator).filter(_.isDisplayedAndEnabled)
      }

  }

  implicit class RichWait[T](fluentWait: FluentWait[T]) {

    class Applyable(description: String) {
      def apply[R >: Null](isTrue: ⇒ Option[R]): R = {
        case class Wrapper(contents: R) // In case R happens to contain null or false, which are interpreted specially by until()
        try {
          val wrapper = fluentWait.until(new GFunction[T, Wrapper]() {
            def apply(input: T): Wrapper = isTrue.map(Wrapper).getOrElse(null)
          })
          wrapper.contents
        } catch {
          case e: TimeoutException ⇒
            throw new TimeoutException(s"Timed out waiting for '$description'", e)
        }
      }
    }

    def waitUntil(description: String) = new Applyable(description)

  }
}