package com.thetestpeople.trt.webdriver.screens

import play.api.test.TestBrowser
import org.openqa.selenium.WebDriver
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.By._
import org.openqa.selenium.WebElement
import com.thetestpeople.trt.utils.Utils
import org.openqa.selenium.By

object SearchLogScreen {

  val QueryFieldLocator = By.id("query-field")

}

class SearchLogsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  import SearchLogScreen._

  webDriver.waitForDisplayedAndEnabled(id("page-SearchLogs"))

  def query: String =
    webDriver.waitForDisplayedAndEnabled(QueryFieldLocator).getAttribute("value")

  def query_=(text: String) = {
    log(s"Set search query: text")
    val elem = webDriver.waitForDisplayedAndEnabled(QueryFieldLocator)
    elem.clear()
    elem.sendKeys(text)
  }

  def clickSearch() {
    log("Click 'Search'")
    webDriver.waitForDisplayedAndEnabled(By.id("search-button")).click()
  }

  def executionRows: List[ExecutionRow] = {
    val executionRowElements = webDriver.findElements_(cssSelector("tr.execution-row"))
    val fragmentRowElements = webDriver.findElements_(cssSelector("tr.fragment-row"))

    for (((rowElement, fragmentElement), index) ← executionRowElements.zip(fragmentRowElements).zipWithIndex)
      yield ExecutionRow(rowElement, fragmentElement, index)
  }

  case class ExecutionRow(rowElement: WebElement, fragmentElement: WebElement, index: Int) {

    private def ordinal = Utils.ordinalName(index + 1)

    def fragmentText: String = fragmentElement.getText
    
  }

}