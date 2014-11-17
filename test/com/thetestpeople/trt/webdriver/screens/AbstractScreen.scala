package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By._

object AbstractScreen {

  private object Locators {
    val AlertSuccessMessage = cssSelector("#alert-success span.alert-message")
  }

}

abstract class AbstractScreen(implicit protected val automationContext: AutomationContext)
    extends AbstractComponent with HasMainMenu {

  import AbstractScreen._

  def successMessageOpt: Option[String] =
    webDriver.findImmediateDisplayedAndEnabled(Locators.AlertSuccessMessage).map(_.getText)

  def waitForSuccessMessage() =
    webDriver.webDriverWait().waitUntil("Success message is displayed") {
      webDriver.findImmediateDisplayedAndEnabled(Locators.AlertSuccessMessage)
    }

  def waitForValidationError() {
    webDriver.waitForDisplayedAndEnabled(className("alert-danger"))
  }

  def refresh() = webDriver.navigate().refresh()

}