package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import org.openqa.selenium._
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.By._
import com.thetestpeople.trt.utils.Utils

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
      log(s"Click 'Last passed' link for the $ordinalName row")
      rowElement.findElement(cssSelector("a.last-passed-link")).click()
      new ExecutionScreen
    }

    def clickTestLink(): TestScreen = {
      log(s"Click the test link for the $ordinalName row")
      rowElement.findElement(cssSelector("a.test-link")).click()
      new TestScreen
    }

    private def ordinalName = Utils.ordinalName(index + 1)

    def name: String = rowElement.findElement(cssSelector("a.test-link")).getAttribute("title")

    private def getCheckBox = rowElement.findElement(cssSelector(".testCheckbox"))

    def selected: Boolean = getCheckBox.isSelected

    def selected_=(value: Boolean) {
      if (value)
        log(s"Select the $ordinalName row")
      else
        log(s"Unselect the $ordinalName row")

      val checkBox = getCheckBox
      val currentlySelected = checkBox.isSelected
      if (currentlySelected != value)
        checkBox.click()
    }

  }

  def clickRerunSelectedTests() {
    log("Click 'Rerun Selected'")
    webDriver.waitForDisplayedAndEnabled(id("rerunSelected")).click()
  }

  def warningTab(): TestsScreen = {
    log(s"Click the 'Warning' tab")
    webDriver.waitForDisplayedAndEnabled(id("warning-tab-link")).click()
    new TestsScreen
  }

}
