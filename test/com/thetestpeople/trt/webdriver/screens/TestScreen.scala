package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.model.Configuration
import org.openqa.selenium.support.ui.Select

class TestScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  webDriver.waitForDisplayedAndEnabled(id("page-Test"))

  def name: String = webDriver.waitForDisplayedAndEnabled(id("name")).getText

  def groupOpt: Option[String] = webDriver.findImmediateDisplayedAndEnabled(id("group")).map(_.getText)

  def configurationOpt: Option[Configuration] =
    webDriver.findImmediateDisplayedAndEnabled(id("configuration-select")).map { select ⇒
      Configuration(new Select(select).getFirstSelectedOption.getText)
    }

  def comment: String = webDriver.findImmediate(id("comment-text")).map(_.getText).getOrElse("")

  def editComment(): CommentDialog[TestScreen] = {
    log("Click 'Edit comment'")
    webDriver.waitForDisplayedAndEnabled(id("edit-comment-link")).click()
    new CommentDialog[TestScreen]() { def returnScreen = new TestScreen }
  }

  def executionRows: List[ExecutionRow] =
    for ((rowElement, index) ← webDriver.findElements_(cssSelector("tr.execution-row")).zipWithIndex)
      yield ExecutionRow(rowElement, index)

  def delete() {
    log("Click 'Mark test as deleted'")
    webDriver.waitForDisplayedAndEnabled(id("delete-test")).click()
    webDriver.waitForDisplayedAndEnabled(id("alert-success"))
  }

  case class ExecutionRow(rowElement: WebElement, index: Int) {

    private def ordinal = Utils.ordinalName(index + 1)

    def passed: Boolean = {
      val imgElement = rowElement.findElement(cssSelector(".pass-fail-icon img"))
      imgElement.getAttribute("title") == "Passed"
    }

    def viewExecution(): ExecutionScreen = {
      log(s"View the $ordinal execution")
      val executionLinkElement =
        rowElement.findImmediate(cssSelector("a.execution-link")).getOrElse(
          throw new RuntimeException("Could not find execution link"))
      executionLinkElement.click()
      new ExecutionScreen
    }

  }

}