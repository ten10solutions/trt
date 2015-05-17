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

  def executionRows: Seq[ExecutionRow] =
    for ((rowElement, index) ← webDriver.findElements_(cssSelector("tr.execution-row")).zipWithIndex)
      yield ExecutionRow(rowElement, index)

  def delete() {
    log("Click 'Mark test as deleted'")
    webDriver.waitForDisplayedAndEnabled(id("delete-test")).click()
    waitForSuccessMessage()
  }

  def ignore() {
    log("Click 'Ignore test'")
    webDriver.waitForDisplayedAndEnabled(id("ignore-test")).click()
    waitForSuccessMessage()
  }

  def unignore() {
    log("Click 'Unignore test'")
    webDriver.waitForDisplayedAndEnabled(id("unignore-test")).click()
    waitForSuccessMessage()
  }

  def categoryWidgets: Seq[CategoryWidget] = webDriver.findElements_(cssSelector(".category-widget")).map(CategoryWidget)

  def clickAddCategory(): AddCategoryDialog = {
    log("Click 'Add' (category)")
    webDriver.waitForDisplayedAndEnabled(id("add-category-link")).click()
    webDriver.waitForDisplayedAndEnabled(id("category-dialog"))
    new AddCategoryDialog
  }

  class AddCategoryDialog {

    private def categoryField = webDriver.waitForDisplayedAndEnabled(id("category-field"))

    def category: String = categoryField.getText

    def category_=(category: String) {
      log(s"Set 'Category': $category")
      val field = categoryField
      field.clear()
      field.sendKeys(category)
    }

    def clickSave() {
      log(s"Click 'Save'")
      webDriver.waitForDisplayedAndEnabled(id("save-category-button")).click()
      waitForSuccessMessage()
    }

  }

  case class CategoryWidget(categoryElement: WebElement) {

    lazy val categoryLink: WebElement = categoryElement.findElement(cssSelector(".category-link"))

    def categoryName: String = categoryLink.getText

    def clickRemoveCategory() {
      log(s"Click 'Remove' for category '$categoryName'")
      categoryElement.findElement(cssSelector(".remove-category-link")).click()
      waitForSuccessMessage()
    }

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