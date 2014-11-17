package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import org.openqa.selenium.By

class ImportLogScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  def clickSyncButton() {
    log("Click 'Sync'")
    webDriver.waitForDisplayedAndEnabled(id("sync-button")).click()
  }
  
  def buildRows: Seq[BuildRow] = webDriver.findElements_(cssSelector("tr.build-row")).map(BuildRow)

  case class BuildRow(rowElement: WebElement) {

    def isSuccess = rowElement.findImmediate(cssSelector(".progress-bar-success")).isDefined

  }

}