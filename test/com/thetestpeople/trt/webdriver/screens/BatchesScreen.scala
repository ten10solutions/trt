package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import org.openqa.selenium.By
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import com.thetestpeople.trt.utils.Utils

class BatchesScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  webDriver.waitForDisplayedAndEnabled(id("page-Batches"))

  def batchRows: Seq[BatchRow] =
    for ((rowElement, index) ← webDriver.findElements_(cssSelector("tr.batch-row")).zipWithIndex)
      yield BatchRow(rowElement, index)

  case class BatchRow(rowElement: WebElement, index: Int) {

    private def ordinal = Utils.ordinalName(index + 1)

    def passed: Boolean = {
      val imgElement = rowElement.findElement(cssSelector(".pass-fail-icon img"))
      imgElement.getAttribute("title") == "Passed"
    }

    def nameOpt: Option[String] = rowElement.findImmediate(cssSelector(".batch-name-cell a")).map(_.getText)

    private def getCount(cellIndex: Int): Int = {
      val cellElement = rowElement.findElements_(cssSelector(s"td.counts-cell"))(cellIndex)
      cellElement.findImmediate(cssSelector("span")).map(_.getText.toInt).getOrElse(0)
    }

    def passes: Int = getCount(cellIndex = 0)

    def fails: Int = getCount(cellIndex = 1)

    def total: Int = getCount(cellIndex = 2)

    def viewBatch(): BatchScreen = {
      log(s"View the $ordinal batch (click its pass/fail icon)")
      rowElement.findElement(cssSelector(".pass-fail-icon")).click()
      new BatchScreen
    }

  }

}