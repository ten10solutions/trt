package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import com.thetestpeople.trt.utils.Utils
import org.openqa.selenium.By

class ExecutionScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  webDriver.waitForDisplayedAndEnabled(id("page-Execution"))

  def configuration: String = getText(id("configuration-link"))

  def logOpt: Option[String] = getTextOpt(id("log"))

  def testName: String = getText(id("test-name"))

  def testGroupOpt: Option[String] = getTextOpt(id("test-group"))

  def batchName: String = getText(id("batch-name"))

  def summaryOpt: Option[String] = getTextOpt(id("execution-summary"))

  private def getText(locator: By) = webDriver.waitForDisplayedAndEnabled(locator).getText

  private def getTextOpt(locator: By) = webDriver.findImmediateDisplayedAndEnabled(locator).map(_.getText)

  def comment: String = webDriver.findImmediate(id("comment-text")).map(_.getText).getOrElse("")

  def editComment(): CommentDialog[ExecutionScreen] = {
    log("Click 'Edit comment'")
    webDriver.waitForDisplayedAndEnabled(id("edit-comment-link")).click()
    new CommentDialog[ExecutionScreen]() { def returnScreen = new ExecutionScreen }
  }

}