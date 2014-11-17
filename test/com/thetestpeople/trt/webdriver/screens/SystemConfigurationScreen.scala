package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id
import org.openqa.selenium.By

object SystemConfigurationScreen {

  private object Locators {
    val BrokenDurationThreshold = id("brokenDurationThreshold")
    val BrokenCountThreshold = id("brokenCountThreshold")
    val HealthyDurationThreshold = id("healthyDurationThreshold")
    val HealthyCountThreshold = id("healthyCountThreshold")

    val ErrorsForBrokenDurationThreshold = id("errors-brokenDurationThreshold")
    val ErrorsForBrokenCountThreshold = id("errors-brokenCountThreshold")
    val ErrorsForHealthyDurationThreshold = id("errors-healthyDurationThreshold")
    val ErrorsForHealthyCountThreshold = id("errors-healthyCountThreshold")

  }

}

class SystemConfigurationScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  import SystemConfigurationScreen._

  def brokenDurationThreshold: String = getField(Locators.BrokenDurationThreshold)
  def brokenCountThreshold: String = getField(Locators.BrokenCountThreshold)
  def healthyDurationThreshold: String = getField(Locators.HealthyDurationThreshold)
  def healthyCountThreshold: String = getField(Locators.HealthyCountThreshold)

  def brokenDurationThreshold_=(text: String) =
    setField("Duration threshold for Broken status", Locators.BrokenDurationThreshold, text)
  def brokenCountThreshold_=(text: String) =
    setField("Count threshold for Broken status", Locators.BrokenCountThreshold, text)
  def healthyDurationThreshold_=(text: String) =
    setField("Duration threshold for Healthy status", Locators.HealthyDurationThreshold, text)
  def healthyCountThreshold_=(text: String) =
    setField("Count threshold for Healthy status", Locators.HealthyCountThreshold, text)

  def errorsForBrokenDurationThreshold = getOptionalText(Locators.ErrorsForBrokenDurationThreshold)
  def errorsForBrokenCountThreshold = getOptionalText(Locators.ErrorsForBrokenCountThreshold)
  def errorsForHealthyDurationThreshold = getOptionalText(Locators.ErrorsForHealthyDurationThreshold)
  def errorsForHealthyCountThreshold = getOptionalText(Locators.ErrorsForHealthyCountThreshold)

  private def getOptionalText(locator: By): Option[String] = webDriver.findImmediate(locator).map(_.getText)

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