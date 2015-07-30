package com.thetestpeople.trt.webdriver.screens

import org.openqa.selenium.WebDriver
import org.openqa.selenium._
import com.thetestpeople.trt.webdriver.screens.RichSelenium._
import org.openqa.selenium.WebElement
import org.openqa.selenium.By
import org.openqa.selenium.By._
import com.thetestpeople.trt.utils.Utils
import com.thetestpeople.trt.model.TestStatus
import com.thetestpeople.trt.utils.StringUtils

object TestsScreen {

  val DefaultPageSize = 12

}

class TestsScreen(implicit automationContext: AutomationContext) extends AbstractScreen with HasMainMenu {

  def total: Int = webDriver.waitForDisplayedAndEnabled(id("total-test-count")).getText.toInt

  def healthy: Int = webDriver.waitForDisplayedAndEnabled(id("healthy-test-count")).getText.toInt

  def ignored: Int = webDriver.waitForDisplayedAndEnabled(id("ignored-test-count")).getText.toInt

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

    private def ordinalName = StringUtils.ordinalName(index + 1)

    def name: String = rowElement.findElement(cssSelector("a.test-link")).getAttribute("title")

    def statusOpt: Option[TestStatus] = Option(rowElement.getAttribute("data-status")).map(TestStatus.parse)

    def isIgnored: Boolean = rowElement.getAttribute("data-ignored").toBoolean

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

  def lastPageNumberDisplayed: Option[Int] =
    webDriver.findElements_(cssSelector("a.directPageLink")).map(_.getText.toInt).sorted.lastOption

  def clickNextPage() {
    log("Click 'Next »'")
    webDriver.waitForDisplayedAndEnabled(cssSelector("a.nextPage")).click()
  }

  def clickPreviousPage() {
    log("Click '« Previous'")
    webDriver.waitForDisplayedAndEnabled(cssSelector("a.previous")).click()
  }

  def clickRerunSelectedTests() {
    log("Click 'Rerun selected'")
    webDriver.waitForDisplayedAndEnabled(id("rerunSelected")).click()
    waitForSuccessMessage()
  }

  def clickIgnoreSelectedTests() {
    log("Click 'Ignore selected'")
    webDriver.waitForDisplayedAndEnabled(id("ignoreSelected")).click()
    waitForSuccessMessage()
  }

  def clickUnignoreSelectedTests() {
    log("Click 'Unignore selected'")
    webDriver.waitForDisplayedAndEnabled(id("unignoreSelected")).click()
    waitForSuccessMessage()
  }

  def selectWarningTab(): TestsScreen = {
    log(s"Click the 'Warning' tab")
    webDriver.waitForDisplayedAndEnabled(id("warning-tab-link")).click()
    webDriver.waitForDisplayedAndEnabled(cssSelector("#warning-tab.active"))
    new TestsScreen
  }

  def selectHealthyTab(): TestsScreen = {
    log(s"Click the 'Healthy' tab")
    webDriver.waitForDisplayedAndEnabled(id("healthy-tab-link")).click()
    webDriver.waitForDisplayedAndEnabled(cssSelector("#healthy-tab.active"))
    new TestsScreen
  }

  def selectIgnoredTab(): TestsScreen = {
    log(s"Click the 'Ignored' tab")
    webDriver.waitForDisplayedAndEnabled(id("ignored-tab-link")).click()
    webDriver.waitForDisplayedAndEnabled(cssSelector("#ignored-tab.active"))
    new TestsScreen
  }

}
