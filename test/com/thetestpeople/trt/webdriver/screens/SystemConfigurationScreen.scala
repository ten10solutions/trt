package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id
import org.openqa.selenium.By

object SystemConfigurationScreen {

  private object Locators {
    val FailureDurationThreshold = id("failureDurationThreshold")
    val FailureCountThreshold = id("failureCountThreshold")
    val PassDurationThreshold = id("passDurationThreshold")
    val PassCountThreshold = id("passCountThreshold")
  }

}

class SystemConfigurationScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  import SystemConfigurationScreen._

  def failureDurationThreshold: String = getField(Locators.FailureDurationThreshold)
  def failureCountThreshold: String = getField(Locators.FailureCountThreshold)
  def passDurationThreshold: String = getField(Locators.PassDurationThreshold)
  def passCountThreshold: String = getField(Locators.PassCountThreshold)

  def failureDurationThreshold_=(text: String) =
    setField("Failure duration threshold", Locators.FailureDurationThreshold, text)
  def failureCountThreshold_=(text: String) =
    setField("Failure count threshold", Locators.FailureCountThreshold, text)
  def passDurationThreshold_=(text: String) =
    setField("Pass duration threshold", Locators.PassDurationThreshold, text)
  def passCountThreshold_=(text: String) =
    setField("Pass count threshold", Locators.PassCountThreshold, text)

  private def getField(locator: By): String =
    webDriver.waitForDisplayedAndEnabled(locator).getAttribute("value")

  private def setField(fieldName: String, locator: By, text: String) {
    log(s"Set '$fieldName': $text")
    val elem = webDriver.waitForDisplayedAndEnabled(locator)
    elem.clear()
    elem.sendKeys(text)
  }

  def clickUpdate() {
    log("Click 'Update'")
    webDriver.waitForDisplayedAndEnabled(id("update")).click()
  }

}