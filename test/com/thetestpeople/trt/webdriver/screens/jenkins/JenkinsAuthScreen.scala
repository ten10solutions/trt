package com.thetestpeople.trt.webdriver.screens.jenkins

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.AutomationContext
import com.thetestpeople.trt.webdriver.screens.AbstractScreen
import com.thetestpeople.trt.webdriver.screens.HasMainMenu
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id

object JenkinsAuthScreen {

  val UsernameLocator = id("credentials_username")

  val ApiTokenLocator = id("credentials_apiToken")

}

class JenkinsAuthScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu with JenkinsConfigurationScreen {

  import JenkinsAuthScreen._

  def username: String =
    webDriver.waitForDisplayedAndEnabled(UsernameLocator).getAttribute("value")

  def username_=(username: String) = {
    log(s"Set 'Username': $username")
    val elem = webDriver.waitForDisplayedAndEnabled(UsernameLocator)
    elem.clear()
    elem.sendKeys(username)
  }

  def apiToken: String =
    webDriver.waitForDisplayedAndEnabled(ApiTokenLocator).getAttribute("value")

  def apiToken_=(apiToken: String) = {
    log(s"Set 'API token': $apiToken")
    val elem = webDriver.waitForDisplayedAndEnabled(ApiTokenLocator)
    elem.clear()
    elem.sendKeys(apiToken)
  }

  def clickSubmit() = {
    log("Click 'Submit'")
    webDriver.waitForDisplayedAndEnabled(id("submit")).click()
    this
  }

}