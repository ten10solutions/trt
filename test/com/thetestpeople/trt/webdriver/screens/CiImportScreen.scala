package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By.id

object CiImportScreen {
  
  val JobUrlLocator = id("jobUrl")
  
}

class CiImportScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  import CiImportScreen._
  
  def jobUrl: String =
    webDriver.waitForDisplayedAndEnabled(JobUrlLocator).getAttribute("value")

  def jobUrl_=(value: String) = {
    log(s"Set 'Job URL': $value")
    val elem = webDriver.waitForDisplayedAndEnabled(JobUrlLocator)
    elem.clear()
    elem.sendKeys(value)
  }
  
  def clickCreate(): ImportLogScreen = {
    log("Click 'Create'")
    webDriver.waitFor(id("submit-button")).click()
    new ImportLogScreen
  }

}