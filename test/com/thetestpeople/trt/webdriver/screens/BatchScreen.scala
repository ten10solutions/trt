package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import com.thetestpeople.trt.utils.Utils
import org.openqa.selenium.By

class BatchScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  webDriver.waitForDisplayedAndEnabled(id("page-Batch"))

  def nameOpt: Option[String] = webDriver.findImmediate(id("batch-name")).map(_.getText)

  def urlOpt: Option[String] = webDriver.findImmediate(id("batch-url")).map(_.getText)

  private def batchLogLinkOpt: Option[WebElement] = webDriver.findImmediate(id("batch-log-link"))

  def hasBatchLog = batchLogLinkOpt.isDefined

  def clickBatchLog() = {
    log("Click 'View' (batch log)")
    batchLogLinkOpt.getOrElse(throw new RuntimeException("No batch log")).click()
    new BatchLogScreen
  }

  def clickDelete(): ConfirmDeleteDialog = {
    log("Click 'Delete batch'")
    webDriver.waitForDisplayedAndEnabled(id("delete-batch-button")).click()
    new ConfirmDeleteDialog
  }

  class ConfirmDeleteDialog(implicit automationContext: AutomationContext) extends AbstractComponent {

    def clickOK(): BatchesScreen = {
      log("Click 'OK'")
      webDriver.waitForDisplayedAndEnabled(By.cssSelector("div.bootbox-confirm button.btn-primary")).click()
      new BatchesScreen
    }

  }

  def executionRows: Seq[ExecutionRow] =
    for ((rowElement, index) ← webDriver.findElements_(cssSelector("tr.execution-row")).zipWithIndex)
      yield ExecutionRow(rowElement, index)

  case class ExecutionRow(rowElement: WebElement, index: Int) {

    private def ordinal = Utils.ordinalName(index + 1)

    def passed: Boolean = {
      val imgElement = rowElement.findElement(cssSelector(".pass-fail-icon img"))
      imgElement.getAttribute("title") == "Passed"
    }

    def groupOpt: Option[String] =
      rowElement.findImmediate(cssSelector(".execution-group")).map(_.getAttribute("title"))

    private def testLinkElement: WebElement =
      rowElement.findImmediate(cssSelector("a.execution-name")).getOrElse(
        throw new RuntimeException("Could not find test name link"))

    def name: String = testLinkElement.getAttribute("title")

    def viewTest(): TestScreen = {
      log(s"View the test of the $ordinal execution")
      testLinkElement.click()
      new TestScreen
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

  def comment: String = webDriver.findImmediate(id("comment-text")).map(_.getText).getOrElse("")

  def editComment(): CommentDialog[BatchScreen] = {
    log("Click 'Edit comment'")
    webDriver.waitForDisplayedAndEnabled(id("edit-comment-link")).click()
    new CommentDialog[BatchScreen]() { def returnScreen = new BatchScreen }
  }

}