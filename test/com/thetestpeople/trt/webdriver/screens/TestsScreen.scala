package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import org.openqa.selenium.By._
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.WebElement
import org.openqa.selenium.By

class TestsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  def total: Int = webDriver.waitForDisplayedAndEnabled(id("total-test-count")).getText.toInt

  def expandFilterOptions: FilterTestsWidget = {
    log("Click the 'Filter Tests' bar to expand the filter options")
    webDriver.waitForDisplayedAndEnabled(id("filter-tests-header-bar")).click()
    webDriver.waitForDisplayedAndEnabled(id("collapse-filter"))
    new FilterTestsWidget
  }

  class FilterTestsWidget {

    def categoryField = webDriver.waitForDisplayedAndEnabled(id("category-field"))

    def category: String = categoryField.getText

    def category_=(category: String) {
      log(s"Set 'Category': $category")
      val field = categoryField
      field.clear()
      field.sendKeys(category)
    }

    def clickSearch() {
      log(s"Click 'Search'")
      webDriver.waitForDisplayedAndEnabled(id("filter-tests")).click()
    }

  }

  def testRows: Seq[TestRow] =
    for ((rowElement, index) ← webDriver.findElements_(cssSelector("tr.test-row")).zipWithIndex)
      yield TestRow(rowElement, index)

  case class TestRow(rowElement: WebElement, index: Int) {

    def clickLastPassedLink(): ExecutionScreen = {
      log(s"Click 'Last passed' link for the ${index + 1}th row")
      rowElement.findElement(By.cssSelector("a.last-passed-link")).click()
      new ExecutionScreen
    }

    def clickTestLink(): TestScreen = {
      log(s"Click the test link for the ${index + 1}th row")
      rowElement.findElement(By.cssSelector("a.test-link")).click()
      new TestScreen
    }

  }

}
