package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.By._
import com.thetestpeople.trt.webdriver.screens.RichSelenium._

abstract class CommentDialog[Screen <: AbstractScreen](implicit automationContext: AutomationContext) extends AbstractComponent {

  webDriver.waitForDisplayedAndEnabled(id("comment-dialog"))

  private def commentTextLocator = id("comment-text")

  def text = webDriver.waitForDisplayedAndEnabled(commentTextLocator).getText()

  def text_=(text: String) {
    webDriver.waitForDisplayedAndEnabled(commentTextLocator).sendKeys(text)
  }

  def returnScreen: Screen

  def cancel(): TestScreen = {
    log("Click 'Cancel'")
    webDriver.waitForDisplayedAndEnabled(id("cancel-button")).click()
    new TestScreen
  }

  def save(): Screen = {
    log("Click 'Save'")
    webDriver.waitForDisplayedAndEnabled(id("save-comment-button")).click()
    webDriver.waitForDisplayedAndEnabled(id("alert-success"))
    returnScreen
  }

}
